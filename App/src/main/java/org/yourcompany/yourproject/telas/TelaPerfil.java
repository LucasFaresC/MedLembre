package org.yourcompany.yourproject.telas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.yourcompany.yourproject.CsvUtil;
import org.yourcompany.yourproject.GerenciadorUsuario;
import org.yourcompany.yourproject.Usuario;

/**
 * Cadastro, edição e exclusão do perfil do usuário (RF01 a RF08).
 * Recebendo usuario == null, funciona como a tela de primeiro cadastro.
 */
public class TelaPerfil extends TelaBase {

    // Aceita "20/05/1950" e também "20/5/1950"
    private static final DateTimeFormatter FORMATO_LEITURA =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    // O RF03 pede dd/mm/aa: "20/05/50" também vale, como 1950 (os usuários são idosos)
    private static final DateTimeFormatter FORMATO_LEITURA_ANO_CURTO = new DateTimeFormatterBuilder()
            .appendPattern("d/M/").appendValueReduced(ChronoField.YEAR, 2, 2, 1900)
            .toFormatter().withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_EXIBICAO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int TAMANHO_FOTO = 150;          // tamanho da foto na tela
    private static final int TAMANHO_FOTO_SALVA = 500;    // tamanho da foto guardada (RF04: 500x500)

    private final GerenciadorUsuario gerenciadorUsuario;
    private Usuario usuario;                    // null = cadastro novo
    private final Consumer<Usuario> aoSalvar;   // o que fazer depois de salvar
    private final Runnable aoExcluir;           // o que fazer depois de excluir o perfil
    private final Runnable aoCancelar;          // o que fazer ao sair sem salvar (pode ser null)

    private JTextField txtNome;
    private JTextField txtNascimento;
    private JComboBox<String> comboGenero;
    private JLabel lblFoto;
    private String fotoPath;

    public TelaPerfil(GerenciadorUsuario gerenciadorUsuario, Usuario usuario,
                      Consumer<Usuario> aoSalvar, Runnable aoExcluir, Runnable aoCancelar) {
        super(usuario == null ? "Cadastro" : "Meu Perfil", 900, 540);
        this.gerenciadorUsuario = gerenciadorUsuario;
        this.usuario = usuario;
        this.aoSalvar = aoSalvar;
        this.aoExcluir = aoExcluir;
        this.aoCancelar = aoCancelar;

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                voltar();
            }
        });
    }

    @Override
    public void inicializarComponentes() {
        String titulo = (usuario == null) ? "Bem-vindo(a)! Vamos fazer o seu cadastro" : "Meu Perfil";
        conteudo.add(criarTitulo(titulo), BorderLayout.NORTH);

        txtNome = new JTextField(18);
        txtNascimento = new JTextField(10);
        comboGenero = new JComboBox<>(new String[]{"Feminino", "Masculino"});

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        adicionarLinha(formulario, 0, "Nome completo:", txtNome);
        adicionarLinha(formulario, 1, "Data de nascimento:", txtNascimento);
        adicionarLinha(formulario, 2, "", new JLabel("(dia/mês/ano, ex.: 20/05/1950 ou 20/05/50)"));
        adicionarLinha(formulario, 3, "Gênero biológico:", comboGenero);

        JPanel formularioNoTopo = new JPanel(new BorderLayout());
        formularioNoTopo.setOpaque(false);
        formularioNoTopo.add(formulario, BorderLayout.NORTH);

        // Foto do usuário (RF04)
        lblFoto = new JLabel("Sem foto", SwingConstants.CENTER);
        lblFoto.setPreferredSize(new Dimension(TAMANHO_FOTO, TAMANHO_FOTO));
        lblFoto.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        JButton btnFoto = criarBotao("Escolher foto", CINZA);
        btnFoto.addActionListener(e -> escolherFoto());

        JPanel painelFoto = new JPanel(new BorderLayout(0, 10));
        painelFoto.setOpaque(false);
        painelFoto.add(lblFoto, BorderLayout.CENTER);
        painelFoto.add(btnFoto, BorderLayout.SOUTH);
        JPanel fotoNoTopo = new JPanel(new BorderLayout());
        fotoNoTopo.setOpaque(false);
        fotoNoTopo.add(painelFoto, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(24, 0));
        centro.setOpaque(false);
        centro.add(formularioNoTopo, BorderLayout.CENTER);
        centro.add(fotoNoTopo, BorderLayout.EAST);
        conteudo.add(centro, BorderLayout.CENTER);

        // ---------- Botões ----------
        JButton btnVoltar = criarBotao(usuario == null ? "Cancelar" : "Voltar", CINZA);
        JButton btnSalvar = criarBotao("Salvar", VERDE);
        btnVoltar.addActionListener(e -> voltar());
        btnSalvar.addActionListener(e -> salvar());

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        direita.setOpaque(false);
        direita.add(btnVoltar);
        direita.add(btnSalvar);

        JPanel botoes = new JPanel(new BorderLayout());
        botoes.setOpaque(false);
        botoes.add(direita, BorderLayout.EAST);
        if (usuario != null) {
            // Excluir fica do lado oposto ao Salvar, para não ser clicado sem querer
            JButton btnExcluir = criarBotao("Excluir perfil", VERMELHO);
            btnExcluir.addActionListener(e -> excluir());
            botoes.add(btnExcluir, BorderLayout.WEST);
        }
        conteudo.add(botoes, BorderLayout.SOUTH);
        frame.getRootPane().setDefaultButton(btnSalvar);   // Enter = Salvar
    }

    // Mostra os dados do usuário nos campos (RF07)
    @Override
    public void atualizarTela() {
        if (usuario == null) {
            mostrarFoto(null);
            return;
        }
        txtNome.setText(usuario.getNome());
        if (usuario.getDataNascimento() != null) {
            txtNascimento.setText(usuario.getDataNascimento().format(FORMATO_EXIBICAO));
        }
        comboGenero.setSelectedItem(usuario.getGeneroBiologico());
        mostrarFoto(usuario.getFotoPath());
    }

    // ---------- Salvar (RF01 a RF06) ----------

    private void salvar() {
        String nome = limpar(txtNome.getText());
        if (nome.isEmpty()) {
            mostrarAviso("Digite o seu nome.");
            txtNome.requestFocus();
            return;
        }

        LocalDate nascimento = lerData(txtNascimento.getText().trim());
        if (nascimento == null) {
            mostrarAviso("Data de nascimento inválida.\nUse dia/mês/ano, por exemplo: 20/05/1950 ou 20/05/50");
            txtNascimento.requestFocus();
            return;
        }
        if (nascimento.isAfter(LocalDate.now()) || nascimento.getYear() < 1900) {
            mostrarAviso("Confira o ano da data de nascimento.");
            txtNascimento.requestFocus();
            return;
        }

        String genero = (String) comboGenero.getSelectedItem();

        if (usuario == null) {
            Usuario novo = new Usuario();
            preencher(novo, nome, nascimento, genero);
            if (!gerenciadorUsuario.cadastrarUsuario(novo)) {
                mostrarAviso("Não foi possível salvar. Confira os dados.");
                return;
            }
            usuario = novo;
            mostrarMensagem("Cadastro feito com sucesso!\nSeja bem-vindo(a), " + nome + ".");
        } else {
            preencher(usuario, nome, nascimento, genero);
            gerenciadorUsuario.editarUsuario(usuario);
            mostrarMensagem("Seus dados foram atualizados.");
        }

        fechar();
        aoSalvar.accept(usuario);
    }

    private void preencher(Usuario u, String nome, LocalDate nascimento, String genero) {
        u.setNome(nome);
        u.setDataNascimento(nascimento);
        u.setGeneroBiologico(genero);
        String foto = copiarFoto(fotoPath);
        if (u.getFotoPath() != null && !u.getFotoPath().equals(foto)) {
            apagarCopiaDaFoto(u.getFotoPath());   // a foto foi trocada: a cópia antiga não serve mais
        }
        u.setFotoPath(foto);
    }

    // Apaga uma cópia de foto guardada pelo programa (nunca apaga arquivos de fora da pasta de fotos)
    private static void apagarCopiaDaFoto(String caminho) {
        if (caminho == null) {
            return;
        }
        File arquivo = new File(caminho);
        File pastaFotos = new File(CsvUtil.getPasta(), "fotos");
        if (pastaFotos.getAbsoluteFile().equals(arquivo.getAbsoluteFile().getParentFile())) {
            arquivo.delete();
        }
    }

    // Aceita ano com 4 dígitos (20/05/1950) ou com 2 (20/05/50); devolve null se for inválida
    private static LocalDate lerData(String texto) {
        for (DateTimeFormatter formato : new DateTimeFormatter[]{FORMATO_LEITURA, FORMATO_LEITURA_ANO_CURTO}) {
            try {
                return LocalDate.parse(texto, formato);
            } catch (DateTimeParseException e) {
                // tenta o próximo formato
            }
        }
        return null;
    }

    // ---------- Excluir (RF08) ----------

    private void excluir() {
        // Com um só perfil, os remédios, horários e histórico são dele e também são apagados (RF08)
        String apagados = (gerenciadorUsuario.listarUsuarios().size() == 1)
                ? "Seus dados, contatos de emergência, remédios, horários e histórico serão apagados."
                : "Seus dados e contatos de emergência serão apagados.";
        if (confirmar("Tem certeza que deseja EXCLUIR o seu perfil?\n" + apagados, "Sim, excluir")) {
            gerenciadorUsuario.deletarUsuario(usuario.getId());
            apagarCopiaDaFoto(usuario.getFotoPath());
            mostrarMensagem("Perfil excluído.");
            fechar();
            aoExcluir.run();
        }
    }

    private void voltar() {
        fechar();
        if (aoCancelar != null) {
            aoCancelar.run();
        }
    }

    // ---------- Foto (RF04) ----------

    private void escolherFoto() {
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Escolha uma foto");
        seletor.setFileFilter(new FileNameExtensionFilter("Imagens (jpg, png, gif)", "jpg", "jpeg", "png", "gif"));
        if (seletor.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            mostrarFoto(seletor.getSelectedFile().getAbsolutePath());
        }
    }

    // Guarda uma cópia da foto na pasta de dados, quadrada e em 500x500 (RF04). Assim a foto
    // não some se o arquivo original for movido ou apagado. Devolve o caminho da cópia.
    private String copiarFoto(String origem) {
        String fotoAtual = (usuario == null) ? null : usuario.getFotoPath();
        if (origem == null) {
            return null;
        }
        File pastaFotos = new File(CsvUtil.getPasta(), "fotos");
        File arquivo = new File(origem);
        if (!arquivo.exists() || pastaFotos.getAbsoluteFile().equals(arquivo.getAbsoluteFile().getParentFile())) {
            return origem;   // já é uma cópia guardada pelo programa (ou não há o que copiar)
        }
        try {
            BufferedImage original = ImageIO.read(arquivo);
            if (original == null) {
                mostrarAviso("Não foi possível abrir a foto escolhida.");
                return fotoAtual;
            }
            // Recorta o centro em formato quadrado e reduz para 500x500
            int lado = Math.min(original.getWidth(), original.getHeight());
            int x = (original.getWidth() - lado) / 2;
            int y = (original.getHeight() - lado) / 2;
            BufferedImage foto = new BufferedImage(TAMANHO_FOTO_SALVA, TAMANHO_FOTO_SALVA, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = foto.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, TAMANHO_FOTO_SALVA, TAMANHO_FOTO_SALVA);
            g.drawImage(original, 0, 0, TAMANHO_FOTO_SALVA, TAMANHO_FOTO_SALVA, x, y, x + lado, y + lado, null);
            g.dispose();
            pastaFotos.mkdirs();
            File destino = new File(pastaFotos, "foto_" + System.currentTimeMillis() + ".png");
            ImageIO.write(foto, "png", destino);
            return CsvUtil.getPasta() + "/fotos/" + destino.getName();
        } catch (IOException e) {
            mostrarAviso("Não foi possível guardar a foto: " + e.getMessage());
            return fotoAtual;
        }
    }

    // Mostra a foto reduzida, sem distorcer (mantém a proporção)
    private void mostrarFoto(String caminho) {
        fotoPath = caminho;
        if (caminho != null && new File(caminho).exists()) {
            ImageIcon original = new ImageIcon(caminho);
            int largura = original.getIconWidth();
            int altura = original.getIconHeight();
            if (largura > 0 && altura > 0) {
                double escala = Math.min((double) TAMANHO_FOTO / largura, (double) TAMANHO_FOTO / altura);
                Image reduzida = original.getImage().getScaledInstance(
                        (int) (largura * escala), (int) (altura * escala), Image.SCALE_SMOOTH);
                lblFoto.setIcon(new ImageIcon(reduzida));
                lblFoto.setText(null);
                return;
            }
        }
        lblFoto.setIcon(null);
        lblFoto.setText("Sem foto");
    }
}
