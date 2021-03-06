/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.uefs.relogio.control;

import br.uefs.relogio.comunication.Protocolo;
import br.uefs.relogio.exceptions.FalhaAoCriarGrupoException;
import br.uefs.relogio.exceptions.FalhaNoEnvioDaMensagem;
import br.uefs.relogio.model.MensagemEleicao;
import br.uefs.relogio.model.Relogio;
import br.uefs.relogio.view.Home;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Classe responsavel pela ligação entre a parte de modelo, rede e interface
 * grafica
 *
 * @author emerson
 */
public class Controller {

    private static Controller instance;
    private static int id;
    private static int idCoordenador;
    private static Relogio ultimoHorarioEnviado;
    private Relogio relogio;
    private Thread threadRelogio;
    private Home tela;
    private ArrayList<MensagemEleicao> horariosEleicao;
    private Thread aguardarFimEleicao;
    private Thread atualizacaoCoordenacao;
    private Thread verificarRecebimentoAtualizacoes;
    private final int tempoAtualizacao = 1;
    private String ultimaMensagemRecebida;

    /**
     * Construtor da classe
     */
    private Controller() {
        ultimaMensagemRecebida = new String();
    }

    /**
     * Retorna a unica instancia da classe
     *
     * @return
     */
    public static Controller getInstance() {
        if (instance == null) {
            instance = new Controller();
        }
        return instance;
    }

    /**
     * Cria o relogio principal
     *
     * @param hora
     * @param minuto
     * @param segundo
     * @param drift
     */
    public void criarRelogio(int hora, int minuto, int segundo, double drift) {
        relogio = new Relogio(hora, minuto, segundo, drift);
        criarTela();
        solicitarEleicao();
        iniciarContagem();
    }

    /**
     * Retorna o relogio principal
     *
     * @return
     */
    public Relogio getRelogio() {
        return relogio;
    }

    /**
     * Retorna o id desse relogio
     *
     * @return
     */
    public static int getId() {
        return id;
    }

    /**
     * Altera o id desse relogio
     *
     * @param id
     */
    public static void setId(int id) {
        Controller.id = id;
    }

    /**
     * Retorna a ultima mensagem recebida do grupo
     *
     * @return
     */
    public String getUltimaMensagemRecebida() {
        return ultimaMensagemRecebida;
    }

    /**
     * altera a ultima mensagem recebida do grupo;
     *
     * @param ultimaMensagemRecebida
     */
    public void setUltimaMensagemRecebida(String ultimaMensagemRecebida) {
        this.ultimaMensagemRecebida = ultimaMensagemRecebida;
    }

    /**
     * Fornece id do coordenador
     *
     * @return
     */
    public static int getIdCoordenador() {
        return idCoordenador;
    }

    /**
     * Atualiza o este relogio, a partir do horario recebido do coordenador
     */
    public synchronized void atualizarRelogio(int hora, int minuto, int segundo) {
        relogio.atualizar(hora, minuto, segundo);
    }

    /**
     * Cria a tela principal
     */
    public void criarTela() {
        tela = new Home();
        tela.setVisible(true);
    }

    /**
     * retorna a tela principal
     *
     * @return
     */
    public Home getTela() {
        return tela;
    }

    /**
     * verifica se o id é igual ao desse relogio
     *
     * @param id
     * @return
     */
    public boolean isMyId(int id) {
        return this.id == id;
    }

    /**
     * Recebe horario do grupo multicast
     *
     * @param id
     * @param hora
     * @param minuto
     * @param segundo
     */
    public synchronized void receberHorarioEleicao(int id, int hora, int minuto, int segundo) {
        Relogio relogioTemp = new Relogio(hora, minuto, segundo);
        MensagemEleicao me = new MensagemEleicao(id, relogioTemp);
        
        if(!verificarHorarioIdLista(id))
            horariosEleicao.add(me);

    }

    /**
     * Prepara atributos para proxima atualização
     */
    private void prepararNovaEleicao() {
        horariosEleicao = null;
        horariosEleicao = new ArrayList<>();

        pararAtualizacaoCoordenacao();
        pararVerificacaoAtualizacao();

    }

    /**
     * para a thread de atualizacao do coordenador caso exista
     */
    private void pararAtualizacaoCoordenacao() {
        if (atualizacaoCoordenacao != null) {
            atualizacaoCoordenacao.stop();
            atualizacaoCoordenacao = null;
        }
    }

    /**
     * para a thread de verificacao do recebimento de atualizacoes
     */
    private void pararVerificacaoAtualizacao() {
        System.err.println("parou verificacao de recebimento de atualizacao");
        if (verificarRecebimentoAtualizacoes != null) {
            verificarRecebimentoAtualizacoes.stop();
            verificarRecebimentoAtualizacoes = null;
        }
    }

    /**
     * Envia o horario para eleicao
     */
    private void enviarHorarioEleicao() {
        try {

            String[] horario = relogio.toString().split(":");

            int hora = Integer.parseInt(horario[0]);
            int minuto = Integer.parseInt(horario[1]);
            int segundo = Integer.parseInt(horario[2]);

            ultimoHorarioEnviado = new Relogio(hora, minuto, segundo);
            Protocolo.enviarHorarioPorEleicao(id, ultimoHorarioEnviado.toString());
        } catch (FalhaNoEnvioDaMensagem | FalhaAoCriarGrupoException ex) {
            exibirFalha(ex);
        }
    }

    public synchronized void receberSolicitacaoEleicao() {
        prepararNovaEleicao();
        enviarHorarioEleicao();
        aguardarFimEleicao();
    }

    /**
     * Para a contagem do relogio
     */
    private void pararContagem() {
        threadRelogio.stop();
    }

    /**
     * Inicia a contagem do relogio
     */
    private void iniciarContagem() {
        threadRelogio = new Thread(relogio);
        threadRelogio.start();
    }

    /**
     * Exibição de possiveis falhas
     *
     * @param e
     */
    public void exibirFalha(Exception e) {
        JOptionPane.showMessageDialog(null, e.getMessage());
    }

    /**
     * Envia uma solicitação de eleição para o grupo
     */
    public void solicitarEleicao() {
        if(aguardarFimEleicao!=null && aguardarFimEleicao.isAlive()) //verifica se está acontecendo eleição
            return;

        try {
            System.err.println("Solicitei eleição: " + id);

            Protocolo.solicitarEleicao(id);

        } catch (FalhaNoEnvioDaMensagem | FalhaAoCriarGrupoException ex) {
            exibirFalha(ex);
        }
    }

    /**
     * Thread para aguardar o fim da eleição e determinar o novo coordenador
     */
    private void aguardarFimEleicao() {
        aguardarFimEleicao = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(500);
                    verificarNovoCoordenador();
                } catch (InterruptedException ex) {
                }
            }
        };
        aguardarFimEleicao.start();
    }

    /**
     * Verifica quem é o novo coordenador no fim da eleição
     */
    private void verificarNovoCoordenador() {

        Collections.sort(horariosEleicao); //ordena a lista em ordem crescente
        Collections.reverse(horariosEleicao); //muda a lista para ordem decrescente

        System.out.println("tamanhoLista: " + horariosEleicao.size());
        
        MensagemEleicao m = horariosEleicao.get(0); //pega o primeiro horario da lista
        //era aqui
        idCoordenador = m.getId();

        System.out.println("Novo Coordenador: " + m.getId() + " Relogio: " + m.getRelogio()); //imprime o novo coordenador

        if (idCoordenador == id) { //se esse for o novo coordenador
            enviarAtualizacao(tempoAtualizacao); //inicia a thread de atualização
        } else {
            verificarRecebimentoAtualizacoes();
            atualizarRelogio(m.getRelogio().getHora(), m.getRelogio().getMinuto(), m.getRelogio().getSegundo()); //atualiza o relogio
        }
    }

    /**
     * Envia atualizações de horario caso seja o coordenador
     *
     * @param tempo
     */
    private void enviarAtualizacao(int tempo) {
        atualizacaoCoordenacao = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        sleep(tempo * 1000); //Aguarda o tempo definido para atualização
                        Protocolo.enviarHorarioPorCoordenacao(id, relogio.toString());
                    }
                } catch (FalhaNoEnvioDaMensagem | FalhaAoCriarGrupoException ex) {
                    exibirFalha(ex);
                    stop();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        atualizacaoCoordenacao.start();
    }

    /**
     * Atualiza hora apartir do horario recebido do coordenador
     *
     * @param hora
     * @param minuto
     * @param segundo
     */
    public synchronized void receberHorarioCoordenacao(int hora, int minuto, int segundo) {
        pararVerificacaoAtualizacao();

        Relogio r = new Relogio(hora, minuto, segundo);

        System.out.println("valor da comparação: " + relogio.compareTo(r));

        if (relogio.compareTo(r) == 1) { //se o relogio for maior que o do coordenador, solicita eleição
            solicitarEleicao();
        } else {
            atualizarRelogio(hora, minuto, segundo); //atualiza o relogio
            verificarRecebimentoAtualizacoes();//inicia a thread de verificação do recebimento de atualização
        }
    }

    /**
     * Verfica se o coordenador está enviando atualizações
     */
    private void verificarRecebimentoAtualizacoes() {
        verificarRecebimentoAtualizacoes = new Thread() {
            @Override
            public void run() {
                try {
                    System.err.println("entrou na thread de atualização");
                    sleep(3000 * tempoAtualizacao);
                    System.err.println("Saiu da thread de atulização e solicitou eleição");
                    solicitarEleicao();
                } catch (InterruptedException ex) {
                }
            }
        };
        verificarRecebimentoAtualizacoes.start();
    }
    
    /**
     * Verifica horarios duplicados na lista de eleições separando por id
     * @param id
     * @return 
     */
    public boolean verificarHorarioIdLista(int id){
        return horariosEleicao.stream().anyMatch((m) -> (m.getId()==id));
    }

}
