package org.yourcompany.yourproject.telas;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import org.yourcompany.yourproject.GerenciadorAlarme;
import org.yourcompany.yourproject.Lembrete;
import org.yourcompany.yourproject.Remedio;

/**
 * Janela de alarme que aparece na hora do remédio (RF18).
 * Fica na frente das outras janelas e repete um som até a pessoa responder.
 * "JÁ TOMEI" registra a ingestão e dá baixa no estoque;
 * "CANCELAR ALARME" registra que a dose não foi tomada (RF19).
 */
public class TelaNotificacaoSwing extends TelaBase {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int INTERVALO_SOM_MS = 4000;
    // Quando o bip atual começou (0 = nenhum tocando). Evita acumular bips se a placa de som for lenta.
    private static volatile long inicioSom = 0;
    // Alarmes abertos agora (para fechar os de um remédio excluído ou de um perfil excluído)
    private static final List<TelaNotificacaoSwing> abertas = new ArrayList<>();

    private final GerenciadorAlarme gerenciadorAlarme;
    private final Lembrete lembrete;
    private Remedio remedio;
    private Timer somRepetido;
    private boolean respondido = false;   // a dose só pode ser respondida uma vez

    // Atributos do diagrama de classes
    private JButton btnCancelarAlarme;
    private JLabel lblInstrucoes;

    private JButton btnJaTomei;
    private JLabel lblHorario;

    public TelaNotificacaoSwing(GerenciadorAlarme gerenciadorAlarme, Lembrete lembrete) {
        super("Hora do remédio", 780, 470);
        this.gerenciadorAlarme = gerenciadorAlarme;
        this.lembrete = lembrete;
        this.remedio = lembrete.getRemedio();

        frame.setAlwaysOnTop(true);   // aparece por cima de qualquer outra janela
        // Todo JOptionPane trava as outras janelas do programa enquanto está aberto. O alarme
        // fica fora dessa trava: se tocar com uma caixa de diálogo aberta, continua clicável.
        frame.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        // Não rouba o foco do teclado: um espaço digitado em outro lugar não aperta "JÁ TOMEI"
        frame.setAutoRequestFocus(false);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                perguntarAoFechar();
            }
        });
    }

    // Mostra o alarme do remédio e começa a tocar o som
    public void exibirAlerta(Remedio r) {
        if (r != null) {
            this.remedio = r;
        }
        abertas.add(this);
        exibir();
        tocarSom();
        somRepetido = new Timer(INTERVALO_SOM_MS, e -> tocarSom());
        somRepetido.start();
    }

    @Override
    public void inicializarComponentes() {
        conteudo.setBackground(AMARELO_ALERTA);

        JLabel lblTitulo = new JLabel("HORA DO REMÉDIO", SwingConstants.CENTER);
        lblTitulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 42));
        lblTitulo.setForeground(VERMELHO);

        lblHorario = new JLabel("", SwingConstants.CENTER);
        lblHorario.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        lblHorario.setForeground(TEXTO);

        lblInstrucoes = new JLabel("", SwingConstants.CENTER);
        lblInstrucoes.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        lblInstrucoes.setForeground(AZUL);

        JPanel cabecalho = new JPanel(new GridLayout(2, 1, 0, 4));
        cabecalho.setOpaque(false);
        cabecalho.add(lblTitulo);
        cabecalho.add(lblHorario);

        // As instruções ocupam todo o espaço que sobrar (nomes longos quebram em várias linhas)
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        centro.add(cabecalho, BorderLayout.NORTH);
        centro.add(lblInstrucoes, BorderLayout.CENTER);

        btnJaTomei = criarBotao("JÁ TOMEI", VERDE);
        btnJaTomei.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        btnJaTomei.addActionListener(e -> jaTomei());

        btnCancelarAlarme = criarBotao("CANCELAR ALARME", CINZA);
        btnCancelarAlarme.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        btnCancelarAlarme.addActionListener(e -> cancelarAlarme());

        JPanel botoes = new JPanel(new GridLayout(1, 2, 20, 0));
        botoes.setOpaque(false);
        botoes.setPreferredSize(new Dimension(0, 96));
        botoes.add(btnJaTomei);
        botoes.add(btnCancelarAlarme);

        conteudo.add(centro, BorderLayout.CENTER);
        conteudo.add(botoes, BorderLayout.SOUTH);
    }

    @Override
    public void atualizarTela() {
        if (lembrete.getHorario() != null) {
            lblHorario.setText("Horário: " + lembrete.getHorario().format(FORMATO_HORA));
        }
        String instrucoes = (remedio != null)
                ? remedio.getInstrucoesDosagem()
                : "Está na hora de tomar o seu remédio.";
        // HTML para o texto quebrar em mais de uma linha se for comprido
        lblInstrucoes.setText("<html><center>" + escaparHtml(instrucoes) + "</center></html>");
    }

    // ---------- Respostas da pessoa ----------

    private void jaTomei() {
        if (respondido) {
            return;   // clique duplo ou clique enquanto o aviso de estoque está aberto
        }
        respondido = true;
        pararSom();
        gerenciadorAlarme.confirmarIngestao(lembrete);

        // Aproveita para avisar se o remédio está acabando (RF21)
        if (remedio != null && remedio.getEstoqueAtual() <= remedio.getEstoqueMinimo()) {
            mostrarAviso("O remédio " + remedio.getNomeTecnico() + " está acabando.\n"
                    + "Restam " + remedio.getEstoqueAtual() + " " + unidadeEstoque(remedio) + ". Providencie a compra.");
        }
        fechar();
    }

    private void cancelarAlarme() {
        if (respondido) {
            return;
        }
        respondido = true;
        pararSom();
        gerenciadorAlarme.cancelarAlerta(lembrete);
        fechar();
    }

    // Se a pessoa clicar no X, perguntamos em vez de simplesmente sumir com o alarme
    private void perguntarAoFechar() {
        if (respondido) {
            return;
        }
        Object[] opcoes = {"Sim, já tomei", "Não tomei"};
        int resposta = JOptionPane.showOptionDialog(frame, "Você já tomou o remédio?",
                "Hora do remédio", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opcoes, opcoes[0]);
        if (resposta == 0) {
            jaTomei();
        } else if (resposta == 1) {
            cancelarAlarme();
        }
        // Se fechou a pergunta sem responder, o alarme continua aberto
    }

    @Override
    public void fechar() {
        pararSom();
        abertas.remove(this);
        super.fechar();
    }

    // Fecha, sem registrar nada, os alarmes abertos de um remédio que acabou de ser excluído
    public static void fecharAlarmesDoRemedio(int idRemedio) {
        for (TelaNotificacaoSwing tela : new ArrayList<>(abertas)) {
            if (tela.remedio != null && tela.remedio.getId() == idRemedio) {
                tela.respondido = true;
                tela.fechar();
            }
        }
    }

    // Fecha, sem registrar nada, todos os alarmes abertos (perfil excluído)
    public static void fecharTodos() {
        for (TelaNotificacaoSwing tela : new ArrayList<>(abertas)) {
            tela.respondido = true;
            tela.fechar();
        }
    }

    // ---------- Som ----------

    private void pararSom() {
        if (somRepetido != null) {
            somRepetido.stop();
        }
    }

    // Gera um "bip" de 880 Hz pela placa de som. O bip do sistema (Toolkit.beep)
    // não toca em muitos computadores, por isso ele fica só como plano B.
    private static void tocarSom() {
        long agora = System.currentTimeMillis();
        if (inicioSom != 0) {
            if (agora - inicioSom > 5000) {
                Toolkit.getDefaultToolkit().beep();   // placa de som travada: usa o bip do sistema
            }
            return;   // não abre outro som enquanto o anterior não terminar
        }
        inicioSom = agora;
        Thread tocador = new Thread(() -> {
            try {
                float taxa = 44100f;
                byte[] amostras = new byte[(int) (taxa * 0.5)];
                for (int i = 0; i < amostras.length; i++) {
                    double angulo = 2 * Math.PI * 880 * i / taxa;
                    amostras[i] = (byte) (Math.sin(angulo) * 100);
                }
                AudioFormat formato = new AudioFormat(taxa, 8, 1, true, false);
                try (SourceDataLine linha = AudioSystem.getSourceDataLine(formato)) {
                    linha.open(formato);
                    linha.start();
                    linha.write(amostras, 0, amostras.length);
                    linha.drain();
                }
            } catch (Exception e) {
                Toolkit.getDefaultToolkit().beep();
            } finally {
                inicioSom = 0;
            }
        }, "som-do-alarme");
        tocador.setDaemon(true);   // não impede o programa de fechar
        tocador.start();
    }

    private static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
