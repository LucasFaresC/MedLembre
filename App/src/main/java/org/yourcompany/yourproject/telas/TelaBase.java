package org.yourcompany.yourproject.telas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.table.DefaultTableModel;
import org.yourcompany.yourproject.Remedio;

/**
 * Classe-mãe (abstrata) de todas as telas do MedLembre.
 *
 * Concentra o que toda tela precisa: a janela (JFrame), a fonte grande
 * pensada para idosos e o aviso legal no rodapé (RF13). Cada tela filha
 * só precisa implementar inicializarComponentes() e atualizarTela().
 */
public abstract class TelaBase {

    // ---------- Cores (alto contraste) ----------
    public static final Color AZUL = new Color(0x0B4F8A);
    public static final Color VERDE = new Color(0x1B6E20);
    public static final Color VERMELHO = new Color(0xB71C1C);
    public static final Color CINZA = new Color(0xDDDDDD);
    public static final Color DESATIVADO = new Color(0xEDEDED);
    public static final Color AMARELO_ALERTA = new Color(0xFFF1B8);
    public static final Color TEXTO = new Color(0x1A1A1A);

    // ---------- Fontes grandes ----------
    public static final Font FONTE_TEXTO = new Font(Font.SANS_SERIF, Font.PLAIN, 20);
    public static final Font FONTE_DESTAQUE = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    public static final Font FONTE_TITULO = new Font(Font.SANS_SERIF, Font.BOLD, 30);

    public static final String AVISO_LEGAL =
            "Este sistema não substitui a orientação médica profissional.";

    // ---------- Atributos do diagrama de classes ----------
    protected JFrame frame;
    protected Font fonteAcessivel;
    protected JLabel lblAvisoLegal;

    // Painel onde cada tela filha coloca os seus componentes
    protected JPanel conteudo;

    protected TelaBase(String titulo, int largura, int altura) {
        fonteAcessivel = FONTE_TEXTO;

        frame = new JFrame("MedLembre - " + titulo);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(largura, altura);
        frame.setLocationRelativeTo(null);   // centraliza na tela

        conteudo = new JPanel(new BorderLayout(16, 16));
        conteudo.setBackground(Color.WHITE);
        conteudo.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        // Aviso legal fixo no rodapé de todas as telas (RF13)
        lblAvisoLegal = new JLabel(AVISO_LEGAL, SwingConstants.CENTER);
        lblAvisoLegal.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 16));
        lblAvisoLegal.setForeground(new Color(0x555555));
        lblAvisoLegal.setOpaque(true);
        lblAvisoLegal.setBackground(new Color(0xF2F2F2));
        lblAvisoLegal.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(conteudo, BorderLayout.CENTER);
        raiz.add(lblAvisoLegal, BorderLayout.SOUTH);
        frame.setContentPane(raiz);
    }

    // Cada tela monta os seus botões, campos e tabelas aqui
    public abstract void inicializarComponentes();

    // Cada tela recarrega os dados que exibe aqui (por exemplo, depois de salvar)
    public abstract void atualizarTela();

    // Monta a tela, aplica a acessibilidade, carrega os dados e mostra a janela
    public void exibir() {
        inicializarComponentes();
        aplicarAcessibilidadeIdoso();
        atualizarTela();
        frame.setVisible(true);
    }

    public void fechar() {
        frame.dispose();
    }

    // Percorre todos os componentes da janela aumentando as fontes pequenas
    // e deixando as linhas das tabelas mais altas
    protected void aplicarAcessibilidadeIdoso() {
        aplicarEm(frame.getContentPane());
    }

    private void aplicarEm(Component c) {
        Font f = c.getFont();
        if (f != null && f.getSize() < 16) {
            c.setFont(f.deriveFont((float) fonteAcessivel.getSize()));
        }
        if (c instanceof JTable tabela) {
            tabela.setRowHeight(38);
        }
        if (c instanceof Container container) {
            for (Component filho : container.getComponents()) {
                aplicarEm(filho);
            }
        }
    }

    // ==================== Aparência de todo o programa ====================

    // Chamado uma única vez pela App: fontes grandes em TODOS os componentes,
    // inclusive nas caixas do JOptionPane, e botões em português
    public static void configurarAparencia() {
        try {
            // Mesmo visual no Windows e no Linux (e as cores dos botões funcionam nos dois)
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Não foi possível aplicar o visual: " + e.getMessage());
        }

        FontUIResource texto = new FontUIResource(FONTE_TEXTO);
        FontUIResource destaque = new FontUIResource(FONTE_DESTAQUE);
        String[] componentesDeTexto = {
            "Label.font", "TextField.font", "FormattedTextField.font", "TextArea.font",
            "ComboBox.font", "List.font", "Table.font", "Spinner.font", "CheckBox.font",
            "RadioButton.font", "OptionPane.messageFont", "TitledBorder.font", "ToolTip.font"
        };
        for (String chave : componentesDeTexto) {
            UIManager.put(chave, texto);
        }
        UIManager.put("Button.font", destaque);
        UIManager.put("OptionPane.buttonFont", destaque);
        UIManager.put("TableHeader.font", destaque);
        // Mais espaço dentro dos botões padrão (ex.: os do JOptionPane)
        UIManager.put("Button.margin", new InsetsUIResource(10, 22, 10, 22));

        UIManager.put("OptionPane.yesButtonText", "Sim");
        UIManager.put("OptionPane.noButtonText", "Não");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("OptionPane.okButtonText", "OK");
    }

    // ==================== Ajudantes usados pelas telas filhas ====================

    // Botão grande e colorido, fácil de enxergar e de clicar
    protected static JButton criarBotao(String texto, Color corFundo) {
        JButton botao = new JButton(texto);
        botao.setFont(FONTE_DESTAQUE);
        botao.setBackground(corFundo);
        botao.setForeground(corFundo.equals(CINZA) ? TEXTO : Color.WHITE);
        botao.setOpaque(true);
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setBorder(bordaBotao(corFundo));
        // No visual Metal, um botão colorido desativado continua com a cor de ativo e
        // engana o usuário; aqui ele fica cinza-claro enquanto estiver desativado
        botao.addPropertyChangeListener("enabled", e -> {
            Color cor = botao.isEnabled() ? corFundo : DESATIVADO;
            botao.setBackground(cor);
            botao.setBorder(bordaBotao(cor));
            botao.setCursor(Cursor.getPredefinedCursor(botao.isEnabled() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        });
        return botao;
    }

    private static javax.swing.border.Border bordaBotao(Color cor) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cor.darker(), 2),
                BorderFactory.createEmptyBorder(12, 22, 12, 22));
    }

    // Unidade em que o estoque de um remédio é contado (comprimidos ou doses de líquido)
    protected static String unidadeEstoque(Remedio r) {
        return (r.getTipo() == Remedio.TipoRemedio.LIQUIDO) ? "dose(s)" : "comprimido(s)";
    }

    protected static JLabel criarTitulo(String texto) {
        JLabel titulo = new JLabel(texto);
        titulo.setFont(FONTE_TITULO);
        titulo.setForeground(AZUL);
        return titulo;
    }

    // Linha "Rótulo: [campo]" de um formulário montado com GridBagLayout
    protected static void adicionarLinha(JPanel formulario, int linha, String rotulo, JComponent campo) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = linha;
        g.insets = new Insets(8, 4, 8, 12);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0;
        JLabel lbl = new JLabel(rotulo);
        lbl.setFont(FONTE_DESTAQUE);
        formulario.add(lbl, g);

        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        formulario.add(campo, g);
    }

    // Tabela só de leitura: o usuário altera os dados pelos botões, não clicando nas células
    protected static JTable criarTabela(DefaultTableModel modelo) {
        JTable tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowHeight(38);
        tabela.setFillsViewportHeight(true);
        tabela.getTableHeader().setReorderingAllowed(false);
        return tabela;
    }

    protected static DefaultTableModel criarModeloTabela(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
    }

    // O ";" separa as colunas no CSV, então não pode aparecer dentro de um texto
    protected static String limpar(String texto) {
        return texto == null ? "" : texto.trim().replace(";", ",");
    }

    // ==================== Caixas de diálogo (JOptionPane) ====================

    protected void mostrarMensagem(String mensagem) {
        JOptionPane.showMessageDialog(frame, mensagem, "MedLembre", JOptionPane.INFORMATION_MESSAGE);
    }

    protected void mostrarAviso(String mensagem) {
        JOptionPane.showMessageDialog(frame, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    // Pergunta de Sim/Não com o texto do "Sim" bem claro (ex.: "Sim, excluir").
    // O botão já selecionado é o "Não", para evitar exclusões sem querer.
    protected boolean confirmar(String pergunta, String textoSim) {
        Object[] opcoes = {textoSim, "Não"};
        int resposta = JOptionPane.showOptionDialog(frame, pergunta, "Confirmação",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[1]);
        return resposta == 0;
    }
}
