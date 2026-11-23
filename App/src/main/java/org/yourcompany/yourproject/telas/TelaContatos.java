package org.yourcompany.yourproject.telas;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.yourcompany.yourproject.ContatoEmergencia;
import org.yourcompany.yourproject.GerenciadorUsuario;
import org.yourcompany.yourproject.Usuario;

/**
 * Contatos de emergência do usuário: até 3 (RF09), com cadastro, edição (RF10)
 * e remoção. Os dados de cada contato são pedidos num formulário de JOptionPane.
 */
public class TelaContatos extends TelaBase {

    private static final int LIMITE_CONTATOS = 3;
    private static final String[] RELACOES = {
        "Filho(a)", "Cônjuge", "Neto(a)", "Irmão(ã)", "Sobrinho(a)", "Vizinho(a)", "Amigo(a)", "Cuidador(a)"
    };

    private final GerenciadorUsuario gerenciadorUsuario;
    private final Usuario usuario;

    private DefaultTableModel modelo;
    private JTable tabela;
    private JLabel lblLimite;
    private JButton btnAdicionar;

    public TelaContatos(GerenciadorUsuario gerenciadorUsuario, Usuario usuario) {
        super("Contatos de Emergência", 920, 540);
        this.gerenciadorUsuario = gerenciadorUsuario;
        this.usuario = usuario;
    }

    @Override
    public void inicializarComponentes() {
        lblLimite = new JLabel();
        JPanel topo = new JPanel(new GridLayout(2, 1, 0, 4));
        topo.setOpaque(false);
        topo.add(criarTitulo("Contatos de Emergência"));
        topo.add(lblLimite);
        conteudo.add(topo, BorderLayout.NORTH);

        modelo = criarModeloTabela("Nome", "Relação", "Telefone");
        tabela = criarTabela(modelo);
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.rowAtPoint(e.getPoint()) >= 0) {
                    editar();   // clique duplo também edita
                }
            }
        });
        conteudo.add(new JScrollPane(tabela), BorderLayout.CENTER);

        btnAdicionar = criarBotao("Adicionar", VERDE);
        JButton btnEditar = criarBotao("Editar", AZUL);
        JButton btnRemover = criarBotao("Remover", VERMELHO);
        JButton btnVoltar = criarBotao("Voltar", CINZA);
        btnAdicionar.addActionListener(e -> adicionar());
        btnEditar.addActionListener(e -> editar());
        btnRemover.addActionListener(e -> remover());
        btnVoltar.addActionListener(e -> fechar());

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        acoes.setOpaque(false);
        acoes.add(btnRemover);
        acoes.add(btnEditar);
        acoes.add(btnAdicionar);

        JPanel botoes = new JPanel(new BorderLayout());
        botoes.setOpaque(false);
        botoes.add(btnVoltar, BorderLayout.WEST);
        botoes.add(acoes, BorderLayout.EAST);
        conteudo.add(botoes, BorderLayout.SOUTH);
    }

    @Override
    public void atualizarTela() {
        modelo.setRowCount(0);
        for (ContatoEmergencia c : usuario.getContatos()) {
            modelo.addRow(new Object[]{c.getNome(), c.getRelacao(), c.getTelefone()});
        }
        int total = usuario.getContatos().size();
        lblLimite.setText("Você pode cadastrar até " + LIMITE_CONTATOS + " contatos. Cadastrados: "
                + total + " de " + LIMITE_CONTATOS + ".");
        btnAdicionar.setEnabled(total < LIMITE_CONTATOS);
    }

    // ---------- Ações ----------

    private void adicionar() {
        if (usuario.getContatos().size() >= LIMITE_CONTATOS) {
            mostrarAviso("Você já tem " + LIMITE_CONTATOS + " contatos de emergência, que é o máximo.\n"
                    + "Para incluir outro, remova um primeiro.");
            return;
        }
        ContatoEmergencia novo = new ContatoEmergencia();
        if (!preencherNoFormulario(novo, "Novo contato de emergência")) {
            return;
        }
        novo.setId(proximoIdContato());
        if (usuario.adicionarContato(novo)) {
            gerenciadorUsuario.editarUsuario(usuario);   // grava no contatos.csv
            atualizarTela();
            mostrarMensagem(novo.getNome() + " foi cadastrado(a) como contato de emergência.");
        }
    }

    private void editar() {
        ContatoEmergencia c = selecionado();
        if (c != null && preencherNoFormulario(c, "Editar contato")) {
            gerenciadorUsuario.editarUsuario(usuario);
            atualizarTela();
        }
    }

    private void remover() {
        ContatoEmergencia c = selecionado();
        if (c != null && confirmar("Remover " + c.getNome() + " dos seus contatos de emergência?", "Sim, remover")) {
            usuario.removerContato(c.getId());
            gerenciadorUsuario.editarUsuario(usuario);
            atualizarTela();
        }
    }

    private ContatoEmergencia selecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            mostrarAviso("Clique em um contato da lista primeiro.");
            return null;
        }
        return usuario.getContatos().get(linha);
    }

    // Formulário dentro de um JOptionPane. Repete até os dados estarem certos
    // ou a pessoa cancelar. Devolve true se o contato foi preenchido.
    private boolean preencherNoFormulario(ContatoEmergencia c, String titulo) {
        JTextField txtNome = new JTextField(c.getNome() == null ? "" : c.getNome(), 18);
        JTextField txtTelefone = new JTextField(c.getTelefone() == null ? "" : c.getTelefone(), 15);
        JComboBox<String> comboRelacao = new JComboBox<>(RELACOES);
        comboRelacao.setEditable(true);   // permite digitar outra relação
        if (c.getRelacao() != null) {
            comboRelacao.setSelectedItem(c.getRelacao());
        }

        JPanel formulario = new JPanel(new GridBagLayout());
        adicionarLinha(formulario, 0, "Nome:", txtNome);
        adicionarLinha(formulario, 1, "Telefone:", txtTelefone);
        adicionarLinha(formulario, 2, "Relação:", comboRelacao);

        while (true) {
            int resposta = JOptionPane.showConfirmDialog(frame, formulario, titulo,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (resposta != JOptionPane.OK_OPTION) {
                return false;
            }
            String nome = limpar(txtNome.getText());
            String telefone = limpar(txtTelefone.getText());
            String relacao = limpar(String.valueOf(comboRelacao.getEditor().getItem()));

            if (nome.isEmpty()) {
                mostrarAviso("Digite o nome do contato.");
            } else if (telefone.replaceAll("\\D", "").length() < 8) {
                mostrarAviso("Telefone inválido. Digite com DDD, por exemplo: (43) 99999-0000");
            } else {
                c.setNome(nome);
                c.setTelefone(telefone);
                c.setRelacao(relacao.isEmpty() ? "Não informada" : relacao);
                return true;
            }
        }
    }

    // Id único considerando os contatos de todos os usuários (o contatos.csv é um só)
    private int proximoIdContato() {
        int maior = 0;
        for (Usuario u : gerenciadorUsuario.listarUsuarios()) {
            for (ContatoEmergencia c : u.getContatos()) {
                maior = Math.max(maior, c.getId());
            }
        }
        return maior + 1;
    }
}
