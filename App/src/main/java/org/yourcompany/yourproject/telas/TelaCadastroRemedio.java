package org.yourcompany.yourproject.telas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import org.yourcompany.yourproject.GerenciadorAlarme;
import org.yourcompany.yourproject.GerenciadorRemedio;
import org.yourcompany.yourproject.Lembrete;
import org.yourcompany.yourproject.Remedio;

/**
 * Cadastro dos remédios e dos seus horários (RF14 a RF17).
 * À esquerda fica a lista; à direita, o formulário. Clicar em um remédio
 * da lista carrega os dados no formulário para editar ou excluir.
 */
public class TelaCadastroRemedio extends TelaBase {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMATO_LEITURA_HORA = DateTimeFormatter.ofPattern("H:mm");

    private final GerenciadorRemedio gerenciadorRemedio;
    private final GerenciadorAlarme gerenciadorAlarme;
    private Remedio remedioSelecionado;   // null = cadastrando um remédio novo
    private List<Remedio> remediosNaTabela = new ArrayList<>();

    // Atributos do diagrama de classes
    private JTextField txtNome;
    private JComboBox<Remedio.TipoRemedio> comboTipo;

    private JSpinner spinnerDose;
    private JLabel lblUnidade;
    private JSpinner spinnerEstoque;
    private JLabel lblUnidadeEstoque;
    private int estoqueCarregado;   // estoque mostrado quando o remédio foi aberto para edição
    private JTextField txtHorarios;
    private JLabel lblModo;
    private JButton btnExcluir;
    private DefaultTableModel modelo;
    private JTable tabela;

    public TelaCadastroRemedio(GerenciadorRemedio gerenciadorRemedio, GerenciadorAlarme gerenciadorAlarme) {
        super("Meus Remédios", 1180, 680);
        this.gerenciadorRemedio = gerenciadorRemedio;
        this.gerenciadorAlarme = gerenciadorAlarme;
    }

    @Override
    public void inicializarComponentes() {
        conteudo.add(criarTitulo("Meus Remédios"), BorderLayout.NORTH);

        // ---------- Lista de remédios (esquerda) ----------
        modelo = criarModeloTabela("Remédio", "Dose", "Horários", "Estoque");
        tabela = criarTabela(modelo);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(230);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(165);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(190);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(85);
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabela.getSelectedRow() >= 0) {
                carregarNoFormulario(remediosNaTabela.get(tabela.getSelectedRow()));
            }
        });
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setPreferredSize(new Dimension(680, 0));

        // ---------- Formulário (direita) ----------
        txtNome = new JTextField(11);

        comboTipo = new JComboBox<>(Remedio.TipoRemedio.values());
        comboTipo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean selecionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, selecionado, foco);
                setText(valor == Remedio.TipoRemedio.LIQUIDO ? "Líquido" : "Comprimido");
                return this;
            }
        });
        comboTipo.addActionListener(e -> trocarTipo());

        spinnerDose = new JSpinner();
        lblUnidade = new JLabel();
        JPanel painelDose = new JPanel(new BorderLayout(10, 0));
        painelDose.setOpaque(false);
        painelDose.add(spinnerDose, BorderLayout.CENTER);
        painelDose.add(lblUnidade, BorderLayout.EAST);

        spinnerEstoque = new JSpinner(new SpinnerNumberModel(30, 0, 9999, 1));
        lblUnidadeEstoque = new JLabel();
        JPanel painelEstoque = new JPanel(new BorderLayout(10, 0));
        painelEstoque.setOpaque(false);
        painelEstoque.add(spinnerEstoque, BorderLayout.CENTER);
        painelEstoque.add(lblUnidadeEstoque, BorderLayout.EAST);
        txtHorarios = new JTextField(11);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        adicionarLinha(formulario, 0, "Nome:", txtNome);
        adicionarLinha(formulario, 1, "Tipo:", comboTipo);
        adicionarLinha(formulario, 2, "Dose:", painelDose);
        adicionarLinha(formulario, 3, "Estoque:", painelEstoque);
        adicionarLinha(formulario, 4, "Horários:", txtHorarios);

        JLabel lblDica = new JLabel("<html>Separe os horários por vírgula.<br>Exemplo: 08:00, 14:00, 20:00</html>");
        lblDica.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        lblDica.setForeground(new Color(0x555555));

        lblModo = new JLabel();
        lblModo.setFont(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 20));
        lblModo.setForeground(AZUL);

        JPanel painelForm = new JPanel(new BorderLayout(0, 8));
        painelForm.setBackground(new Color(0xF5F8FC));
        painelForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xB8C9DD), 2),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        painelForm.add(lblModo, BorderLayout.NORTH);
        JPanel formularioNoTopo = new JPanel(new BorderLayout());
        formularioNoTopo.setOpaque(false);
        formularioNoTopo.add(formulario, BorderLayout.NORTH);
        painelForm.add(formularioNoTopo, BorderLayout.CENTER);
        painelForm.add(lblDica, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(16, 0));
        centro.setOpaque(false);
        centro.add(rolagem, BorderLayout.CENTER);
        centro.add(painelForm, BorderLayout.EAST);
        conteudo.add(centro, BorderLayout.CENTER);

        // ---------- Botões ----------
        JButton btnVoltar = criarBotao("Voltar", CINZA);
        JButton btnNovo = criarBotao("Novo", CINZA);
        btnExcluir = criarBotao("Excluir", VERMELHO);
        JButton btnSalvar = criarBotao("Salvar", VERDE);
        btnVoltar.addActionListener(e -> fechar());
        btnNovo.addActionListener(e -> limparFormulario());
        btnExcluir.addActionListener(e -> excluirRemedio());
        btnSalvar.addActionListener(e -> salvarRemedio());

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        direita.setOpaque(false);
        direita.add(btnNovo);
        direita.add(btnExcluir);
        direita.add(btnSalvar);

        JPanel botoes = new JPanel(new BorderLayout());
        botoes.setOpaque(false);
        botoes.add(btnVoltar, BorderLayout.WEST);
        botoes.add(direita, BorderLayout.EAST);
        conteudo.add(botoes, BorderLayout.SOUTH);
    }

    @Override
    public void atualizarTela() {
        modelo.setRowCount(0);
        remediosNaTabela = gerenciadorRemedio.listarRemedios();
        for (Remedio r : remediosNaTabela) {
            modelo.addRow(new Object[]{
                r.getNomeTecnico(), r.getDosagemEspecifica(), textoHorarios(r), r.getEstoqueAtual()
            });
        }
        limparFormulario();
    }

    // ---------- Salvar (cadastrar novo ou alterar existente) ----------

    public void salvarRemedio() {
        String nome = limpar(txtNome.getText());
        if (nome.isEmpty()) {
            mostrarAviso("Digite o nome do remédio.");
            txtNome.requestFocus();
            return;
        }
        List<LocalTime> horarios = lerHorarios(txtHorarios.getText());
        if (horarios == null) {
            return;   // o aviso do horário inválido já foi mostrado
        }
        if (horarios.isEmpty()) {
            mostrarAviso("Informe pelo menos um horário para tomar o remédio.");
            txtHorarios.requestFocus();
            return;
        }

        if (!doseValida()) {
            mostrarAviso("A dose digitada não é válida.\n"
                    + "Comprimidos: número inteiro de 1 a 20.\nLíquidos: de 0,5 a 100 ml (exemplo: 2,5).");
            return;
        }

        Remedio.TipoRemedio tipo = (Remedio.TipoRemedio) comboTipo.getSelectedItem();
        Number dose = (Number) spinnerDose.getValue();
        int estoque = (Integer) spinnerEstoque.getValue();
        Remedio r;

        if (remedioSelecionado == null) {
            // Usa os construtores sobrecarregados da classe Remedio (RF14)
            if (tipo == Remedio.TipoRemedio.LIQUIDO) {
                r = new Remedio(nome, dose.doubleValue(), estoque);
            } else {
                r = new Remedio(nome, dose.intValue(), estoque);
            }
            gerenciadorRemedio.cadastrarRemedio(r);
        } else {
            // Alteração (RF17): os horários antigos são trocados pelos novos
            r = remedioSelecionado;
            r.setNomeTecnico(nome);
            r.setTipo(tipo);
            r.setDosagemEspecifica(textoDosagem(tipo, dose));
            // Só troca o estoque se a pessoa mudou o campo: um "JÁ TOMEI" feito com esta tela
            // aberta já baixou o estoque, e o valor antigo do campo não pode desfazer isso
            if (estoque != estoqueCarregado) {
                r.setEstoqueAtual(estoque);
            }
            gerenciadorRemedio.editarRemedio(r);
            gerenciadorAlarme.removerLembretesDoRemedio(r.getId());
        }

        // Horário que já passou hoje só volta a tocar amanhã: a mensagem deixa isso claro
        LocalTime agora = LocalTime.now().withSecond(0).withNano(0);
        List<String> horas = new ArrayList<>();
        for (LocalTime h : horarios) {
            gerenciadorAlarme.cadastrarLembrete(new Lembrete(0, h, true, r));
            horas.add(h.format(FORMATO_HORA) + (h.isBefore(agora) ? " (a partir de amanhã)" : ""));
        }

        mostrarMensagem("Remédio salvo!\n\n" + r.getInstrucoesDosagem()
                + "\nO alarme vai tocar às: " + String.join(", ", horas));
        atualizarTela();
    }

    // ---------- Excluir (RF16) ----------

    private void excluirRemedio() {
        if (remedioSelecionado == null) {
            mostrarAviso("Clique em um remédio da lista primeiro.");
            return;
        }
        if (confirmar("Excluir o remédio \"" + remedioSelecionado.getNomeTecnico()
                + "\" e todos os seus horários?", "Sim, excluir")) {
            TelaNotificacaoSwing.fecharAlarmesDoRemedio(remedioSelecionado.getId());
            gerenciadorAlarme.removerLembretesDoRemedio(remedioSelecionado.getId());
            gerenciadorRemedio.deletarRemedio(remedioSelecionado.getId());
            atualizarTela();
        }
    }

    // ---------- Formulário ----------

    private void limparFormulario() {
        remedioSelecionado = null;
        tabela.clearSelection();
        txtNome.setText("");
        comboTipo.setSelectedItem(Remedio.TipoRemedio.COMPRIMIDO);
        trocarTipo();
        spinnerEstoque.setValue(30);
        txtHorarios.setText("");
        lblModo.setText("Novo remédio");
        btnExcluir.setEnabled(false);
        txtNome.requestFocus();
    }

    private void carregarNoFormulario(Remedio r) {
        remedioSelecionado = r;
        txtNome.setText(r.getNomeTecnico());

        // O tipo vem do remédio; o número da dose vem do texto da dosagem ("1 comprimido(s)" ou "5.0 ml")
        Remedio.TipoRemedio tipo = (r.getTipo() == null) ? Remedio.TipoRemedio.COMPRIMIDO : r.getTipo();
        comboTipo.setSelectedItem(tipo);   // troca o campo da dose antes de colocar o valor
        String dosagem = r.getDosagemEspecifica() == null ? "" : r.getDosagemEspecifica();
        try {
            String numero = dosagem.trim().split(" ")[0];
            if (tipo == Remedio.TipoRemedio.LIQUIDO) {
                spinnerDose.setValue(Double.parseDouble(numero));
            } else {
                spinnerDose.setValue(Integer.parseInt(numero));
            }
        } catch (RuntimeException e) {
            mostrarAviso("Não foi possível ler a dose \"" + dosagem + "\".\nConfira o campo Dose antes de salvar.");
        }

        estoqueCarregado = r.getEstoqueAtual();
        spinnerEstoque.setValue(estoqueCarregado);
        txtHorarios.setText(textoHorarios(r));
        lblModo.setText("Editando: " + r.getNomeTecnico());
        btnExcluir.setEnabled(true);
    }

    // Comprimido: dose inteira (1, 2, 3...). Líquido: dose em ml, de meio em meio.
    private void trocarTipo() {
        Remedio.TipoRemedio tipo = (Remedio.TipoRemedio) comboTipo.getSelectedItem();
        if (tipo == Remedio.TipoRemedio.LIQUIDO) {
            spinnerDose.setModel(new SpinnerNumberModel(5.0, 0.5, 100.0, 0.5));
            lblUnidadeEstoque.setText("dose(s)");
        } else {
            spinnerDose.setModel(new SpinnerNumberModel(1, 1, 20, 1));
            lblUnidadeEstoque.setText("comprimido(s)");
        }
        configurarCampoDose(spinnerDose);
        lblUnidade.setText(tipo.getUnidade());
    }

    // Campo da dose que não depende do idioma do computador. No Brasil o "." separa milhares
    // e "2.5" viraria 25 ml; aqui "2,5" e "2.5" valem dois e meio. Meio comprimido ou valor
    // fora do intervalo é recusado (o salvarRemedio avisa a pessoa).
    private static void configurarCampoDose(JSpinner spinner) {
        SpinnerNumberModel modelo = (SpinnerNumberModel) spinner.getModel();
        boolean decimal = modelo.getValue() instanceof Double;
        DecimalFormat formato = new DecimalFormat(decimal ? "0.0##" : "0", DecimalFormatSymbols.getInstance(Locale.ROOT)) {
            @Override
            public Number parse(String texto, ParsePosition posicao) {
                return super.parse(texto.replace(',', '.'), posicao);
            }
        };
        formato.setGroupingUsed(false);
        formato.setParseIntegerOnly(!decimal);
        NumberFormatter conversor = new NumberFormatter(formato) {
            @Override
            public Object stringToValue(String texto) throws ParseException {
                String limpo = texto.trim();
                ParsePosition posicao = new ParsePosition(0);
                Number numero = formato.parse(limpo, posicao);
                if (numero == null || posicao.getIndex() != limpo.length()) {
                    throw new ParseException(texto, posicao.getIndex());
                }
                double valor = numero.doubleValue();
                if (valor < ((Number) modelo.getMinimum()).doubleValue()
                        || valor > ((Number) modelo.getMaximum()).doubleValue()) {
                    throw new ParseException(texto, 0);
                }
                return decimal ? (Object) valor : (Object) numero.intValue();
            }
        };
        conversor.setValueClass(decimal ? Double.class : Integer.class);
        JFormattedTextField campo = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        campo.setFormatterFactory(new DefaultFormatterFactory(conversor));
        campo.setFocusLostBehavior(JFormattedTextField.COMMIT);   // texto inválido fica visível (não volta sozinho)
        campo.setValue(spinner.getValue());
    }

    // Confirma o que foi digitado na dose; false se o texto não for uma dose válida
    private boolean doseValida() {
        JFormattedTextField campo = ((JSpinner.DefaultEditor) spinnerDose.getEditor()).getTextField();
        try {
            campo.commitEdit();
            return true;
        } catch (ParseException e) {
            campo.requestFocus();
            return false;
        }
    }

    // Mesmo formato de texto usado pelos construtores da classe Remedio
    private static String textoDosagem(Remedio.TipoRemedio tipo, Number dose) {
        if (tipo == Remedio.TipoRemedio.LIQUIDO) {
            return dose.doubleValue() + " ml";
        }
        return dose.intValue() + " comprimido(s)";
    }

    // ---------- Horários ----------

    private List<Lembrete> lembretesDoRemedio(Remedio r) {
        List<Lembrete> lista = new ArrayList<>();
        for (Lembrete l : gerenciadorAlarme.listarLembretes()) {
            if (l.getRemedio() != null && l.getRemedio().getId() == r.getId() && l.getHorario() != null) {
                lista.add(l);
            }
        }
        lista.sort(Comparator.comparing(Lembrete::getHorario));
        return lista;
    }

    private String textoHorarios(Remedio r) {
        List<String> horas = new ArrayList<>();
        for (Lembrete l : lembretesDoRemedio(r)) {
            horas.add(l.getHorario().format(FORMATO_HORA));
        }
        return String.join(", ", horas);
    }

    // Aceita "08:00", "8:00", "8", "8h" e "8h30", separados por vírgula ou espaço.
    // Devolve null (e avisa) se algum horário for inválido.
    private List<LocalTime> lerHorarios(String texto) {
        List<LocalTime> horarios = new ArrayList<>();
        for (String parte : texto.split("[,;\\s]+")) {
            if (parte.isBlank()) {
                continue;
            }
            String hora = parte.trim().toLowerCase().replace("h", ":");
            if (hora.endsWith(":")) {
                hora += "00";
            }
            if (hora.matches("\\d{1,2}")) {
                hora += ":00";   // só a hora ("8") vale como 8:00
            }
            try {
                LocalTime h = LocalTime.parse(hora, FORMATO_LEITURA_HORA);
                if (!horarios.contains(h)) {
                    horarios.add(h);
                }
            } catch (DateTimeParseException e) {
                mostrarAviso("O horário \"" + parte + "\" não é válido.\n"
                        + "Use hora e minuto, por exemplo: 08:00 ou 14:30");
                txtHorarios.requestFocus();
                return null;
            }
        }
        Collections.sort(horarios);
        return horarios;
    }
}
