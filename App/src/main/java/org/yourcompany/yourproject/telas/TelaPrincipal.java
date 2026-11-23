package org.yourcompany.yourproject.telas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import org.yourcompany.yourproject.ContatoEmergencia;
import org.yourcompany.yourproject.CsvUtil;
import org.yourcompany.yourproject.GerenciadorAlarme;
import org.yourcompany.yourproject.GerenciadorRemedio;
import org.yourcompany.yourproject.GerenciadorUsuario;
import org.yourcompany.yourproject.Lembrete;
import org.yourcompany.yourproject.Remedio;
import org.yourcompany.yourproject.Usuario;

/**
 * Tela inicial: saudação, relógio, próximo remédio, botões grandes para cada
 * função e o botão de EMERGÊNCIA sempre visível (RF11 e RF12).
 */
public class TelaPrincipal extends TelaBase {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.of("pt", "BR"));
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final GerenciadorUsuario gerenciadorUsuario;
    private final GerenciadorRemedio gerenciadorRemedio;
    private final GerenciadorAlarme gerenciadorAlarme;
    private final Runnable voltarAoInicio;   // usado quando o perfil é excluído
    private Usuario usuario;

    private JLabel lblSaudacao;
    private JLabel lblData;
    private JLabel lblRelogio;
    private JLabel lblProximoRemedio;
    private JLabel lblEstoque;
    private Timer relogio;
    private TelaBase telaAberta;   // só uma tela de função aberta por vez

    public TelaPrincipal(GerenciadorUsuario gerenciadorUsuario, GerenciadorRemedio gerenciadorRemedio,
                         GerenciadorAlarme gerenciadorAlarme, Usuario usuario, Runnable voltarAoInicio) {
        super("Início", 920, 720);
        this.gerenciadorUsuario = gerenciadorUsuario;
        this.gerenciadorRemedio = gerenciadorRemedio;
        this.gerenciadorAlarme = gerenciadorAlarme;
        this.usuario = usuario;
        this.voltarAoInicio = voltarAoInicio;

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sair();
            }

            // Ao voltar para esta janela (ex.: depois de cadastrar um remédio), os avisos são atualizados
            @Override
            public void windowActivated(WindowEvent e) {
                atualizarTela();
            }
        });
    }

    @Override
    public void inicializarComponentes() {
        // ---------- Topo: saudação, data e relógio ----------
        lblSaudacao = criarTitulo("");
        lblData = new JLabel();
        JPanel textosTopo = new JPanel(new GridLayout(2, 1));
        textosTopo.setOpaque(false);
        textosTopo.add(lblSaudacao);
        textosTopo.add(lblData);

        lblRelogio = new JLabel("", SwingConstants.RIGHT);
        lblRelogio.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 46));
        lblRelogio.setForeground(AZUL);

        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.add(textosTopo, BorderLayout.CENTER);
        topo.add(lblRelogio, BorderLayout.EAST);

        // ---------- Avisos: próximo remédio e estoque ----------
        lblProximoRemedio = new JLabel();
        lblProximoRemedio.setFont(FONTE_DESTAQUE);
        lblEstoque = new JLabel();
        lblEstoque.setFont(FONTE_DESTAQUE);

        JPanel avisos = new JPanel(new GridLayout(2, 1, 0, 6));
        avisos.setBackground(new Color(0xEAF2FB));
        avisos.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xB8C9DD), 2),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        avisos.add(lblProximoRemedio);
        avisos.add(lblEstoque);

        JPanel norte = new JPanel(new BorderLayout(0, 14));
        norte.setOpaque(false);
        norte.add(topo, BorderLayout.NORTH);
        norte.add(avisos, BorderLayout.CENTER);

        // ---------- Botões das funções ----------
        JButton btnRemedios = criarBotao("Meus Remédios", AZUL);
        JButton btnEstoque = criarBotao("Estoque", AZUL);
        JButton btnHistorico = criarBotao("Histórico da Semana", AZUL);
        JButton btnContatos = criarBotao("Contatos de Emergência", AZUL);
        JButton btnPerfil = criarBotao("Meu Perfil", AZUL);
        JButton btnSair = criarBotao("Sair", CINZA);

        btnRemedios.addActionListener(e -> abrir(() -> new TelaCadastroRemedio(gerenciadorRemedio, gerenciadorAlarme)));
        btnEstoque.addActionListener(e -> abrir(() -> new TelaEstoque(gerenciadorRemedio)));
        btnHistorico.addActionListener(e -> abrir(() -> new TelaHistorico(gerenciadorAlarme)));
        btnContatos.addActionListener(e -> abrir(() -> new TelaContatos(gerenciadorUsuario, usuario)));
        btnPerfil.addActionListener(e -> abrir(this::criarTelaPerfil));
        btnSair.addActionListener(e -> sair());

        JPanel grade = new JPanel(new GridLayout(3, 2, 16, 16));
        grade.setOpaque(false);
        grade.add(btnRemedios);
        grade.add(btnEstoque);
        grade.add(btnHistorico);
        grade.add(btnContatos);
        grade.add(btnPerfil);
        grade.add(btnSair);

        // ---------- Emergência (RF11): grande, vermelho e sempre visível ----------
        JButton btnEmergencia = criarBotao("EMERGÊNCIA  -  pedir ajuda aos meus contatos", VERMELHO);
        btnEmergencia.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        btnEmergencia.setPreferredSize(new Dimension(0, 84));
        btnEmergencia.addActionListener(e -> acionarEmergencia());

        conteudo.add(norte, BorderLayout.NORTH);
        conteudo.add(grade, BorderLayout.CENTER);
        conteudo.add(btnEmergencia, BorderLayout.SOUTH);

        // O relógio da tela é atualizado a cada segundo
        relogio = new Timer(1000, e -> atualizarRelogio());
        relogio.start();
    }

    @Override
    public void atualizarTela() {
        String primeiroNome = usuario.getNome().trim().split(" ")[0];
        lblSaudacao.setText("Olá, " + primeiroNome + "!");
        atualizarRelogio();
        lblProximoRemedio.setText(textoProximoRemedio());

        List<Remedio> remedios = gerenciadorRemedio.listarRemedios();
        int acabando = 0;
        for (Remedio r : remedios) {
            if (r.getEstoqueAtual() <= r.getEstoqueMinimo()) {
                acabando++;
            }
        }
        if (remedios.isEmpty()) {
            lblEstoque.setText("Nenhum remédio cadastrado ainda.");
            lblEstoque.setForeground(TEXTO);
        } else if (acabando == 0) {
            lblEstoque.setText("Estoque em dia.");
            lblEstoque.setForeground(VERDE);
        } else {
            lblEstoque.setText("Atenção: " + acabando + " remédio(s) acabando. Veja em \"Estoque\".");
            lblEstoque.setForeground(VERMELHO);
        }
    }

    @Override
    public void fechar() {
        relogio.stop();
        super.fechar();
    }

    // ---------- Navegação ----------

    // Abre uma tela de função. Se já houver uma aberta, ela é trazida para a frente
    // (evita abrir a mesma janela duas vezes com um clique duplo).
    private void abrir(Supplier<TelaBase> criarTela) {
        if (telaAberta != null && telaAberta.frame.isDisplayable()) {
            telaAberta.frame.toFront();
            return;
        }
        telaAberta = criarTela.get();
        telaAberta.exibir();
    }

    private TelaBase criarTelaPerfil() {
        return new TelaPerfil(gerenciadorUsuario, usuario,
                usuarioSalvo -> {
                    usuario = usuarioSalvo;
                    atualizarTela();
                },
                () -> {
                    // Sem nenhum perfil, os remédios, horários e histórico também são apagados (RF08)
                    if (gerenciadorUsuario.listarUsuarios().isEmpty()) {
                        TelaNotificacaoSwing.fecharTodos();
                        gerenciadorAlarme.limparDados();
                        gerenciadorRemedio.limparDados();
                    }
                    fechar();
                    voltarAoInicio.run();
                },
                null);
    }

    // ---------- Emergência (RF11 e RF12) ----------

    private void acionarEmergencia() {
        List<ContatoEmergencia> contatos = usuario.getContatos();
        if (contatos.isEmpty()) {
            mostrarAviso("Você ainda não cadastrou contatos de emergência.\n"
                    + "Use o botão \"Contatos de Emergência\" para cadastrar.");
            return;
        }

        StringBuilder texto = new StringBuilder("Seus contatos de emergência:\n\n");
        for (ContatoEmergencia c : contatos) {
            texto.append("   ").append(c.getNome()).append("  |  ").append(c.getRelacao()).append("  |  ")
                 .append(c.getTelefone()).append("\n");
        }
        texto.append("\nDeseja enviar um pedido de ajuda para eles?");

        Object[] opcoes = {"Sim, pedir ajuda", "Não"};
        int resposta = JOptionPane.showOptionDialog(frame, texto.toString(), "EMERGÊNCIA",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, opcoes, opcoes[0]);
        if (resposta == 0) {
            StringBuilder telefones = new StringBuilder();
            for (ContatoEmergencia c : contatos) {
                c.enviarNotificacao();
                telefones.append("   ").append(c.getNome()).append(":  ").append(c.getTelefone()).append("\n");
            }
            // O envio é simulado (RF12): a pessoa precisa saber disso e ter os números à mão
            mostrarAviso("Pedido de ajuda registrado para " + contatos.size() + " contato(s).\n\n"
                    + "Atenção: nesta versão do sistema o envio da mensagem é simulado.\n"
                    + "Telefone agora para:\n\n" + telefones
                    + "\nEm emergência médica, ligue para o SAMU: 192.");
        }
    }

    // ---------- Textos da tela ----------

    private void atualizarRelogio() {
        lblRelogio.setText(LocalTime.now().format(FORMATO_HORA));
        String data = LocalDate.now().format(FORMATO_DATA);
        lblData.setText(Character.toUpperCase(data.charAt(0)) + data.substring(1));
    }

    // Procura o lembrete ativo mais próximo a partir de agora
    private String textoProximoRemedio() {
        LocalTime agora = LocalTime.now().withSecond(0).withNano(0);
        Lembrete proximo = null;
        long menorEspera = Long.MAX_VALUE;
        boolean proximoEhAmanha = false;

        for (Lembrete l : gerenciadorAlarme.listarLembretes()) {
            if (!l.isAtivo() || l.getHorario() == null || l.getRemedio() == null) {
                continue;
            }
            long espera = Duration.between(agora, l.getHorario().withSecond(0).withNano(0)).toMinutes();
            boolean amanha = espera < 0;   // o horário de hoje já passou
            if (amanha) {
                espera += 24 * 60;
            }
            if (espera < menorEspera) {
                menorEspera = espera;
                proximo = l;
                proximoEhAmanha = amanha;
            }
        }

        if (proximo == null) {
            return "Nenhum horário de remédio cadastrado ainda.";
        }
        return "Próximo remédio: " + proximo.getRemedio().getNomeTecnico() + " às "
                + proximo.getHorario().format(FORMATO_HORA) + (proximoEhAmanha ? " (amanhã)" : "");
    }

    private void sair() {
        if (!CsvUtil.regravarPendentes() && !confirmar("Algumas alterações ainda não foram gravadas, porque um arquivo de dados\n"
                + "está aberto em outro programa (por exemplo, o Excel).\n\n"
                + "Se você sair agora, essas alterações serão perdidas. Sair mesmo assim?", "Sim, sair")) {
            return;
        }
        if (confirmar("Se você fechar o programa, os alarmes dos remédios NÃO vão tocar.\n"
                + "Deseja mesmo sair?", "Sim, sair")) {
            System.exit(0);
        }
    }
}
