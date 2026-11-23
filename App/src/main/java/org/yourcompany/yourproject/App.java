package org.yourcompany.yourproject;

import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.yourcompany.yourproject.telas.TelaBase;
import org.yourcompany.yourproject.telas.TelaNotificacaoSwing;
import org.yourcompany.yourproject.telas.TelaPerfil;
import org.yourcompany.yourproject.telas.TelaPrincipal;

/**
 * Ponto de entrada do MedLembre com interface gráfica (Swing).
 * É a classe configurada no pom.xml (exec.mainClass).
 * Os testes de console da equipe continuam na classe Certificadora.
 */
public class App {

    private static GerenciadorUsuario gerenciadorUsuario;
    private static GerenciadorRemedio gerenciadorRemedio;
    private static GerenciadorAlarme gerenciadorAlarme;

    public static void main(String[] args) {
        // Toda tela Swing deve ser criada na thread de eventos (EDT)
        SwingUtilities.invokeLater(App::iniciarSistema);
    }

    private static void iniciarSistema() {
        TelaBase.configurarAparencia();

        // Se algum arquivo não puder ser lido ou gravado, a pessoa é avisada (e não perde dados sem saber)
        CsvUtil.setAoFalhar(mensagem -> JOptionPane.showMessageDialog(null, mensagem,
                "Problema com os arquivos de dados", JOptionPane.ERROR_MESSAGE));
        // A cada 10 segundos, tenta de novo gravar o que não pôde ser salvo
        new Timer(10000, e -> CsvUtil.regravarPendentes()).start();

        gerenciadorUsuario = new GerenciadorUsuario();
        gerenciadorRemedio = new GerenciadorRemedio();
        gerenciadorAlarme = new GerenciadorAlarme(gerenciadorRemedio);

        // Lê os dados salvos nas vezes anteriores (pasta "dados").
        // Os remédios vêm antes dos lembretes, porque cada lembrete aponta para um remédio.
        gerenciadorUsuario.carregar();
        gerenciadorRemedio.carregar();
        gerenciadorAlarme.carregar();

        // Quando um lembrete tocar, abre a tela de alarme (RF18 e RF19)
        gerenciadorAlarme.setAoDisparar(lembrete ->
                new TelaNotificacaoSwing(gerenciadorAlarme, lembrete).exibirAlerta(lembrete.getRemedio()));
        gerenciadorAlarme.iniciarMonitoramento();

        abrirTelaInicial();
    }

    // Primeiro acesso: cadastro. Um usuário: entra direto. Vários: pergunta quem é.
    private static void abrirTelaInicial() {
        List<Usuario> usuarios = gerenciadorUsuario.listarUsuarios();
        if (usuarios.isEmpty()) {
            new TelaPerfil(gerenciadorUsuario, null, App::abrirPrincipal, null, App::sair).exibir();
        } else if (usuarios.size() == 1) {
            abrirPrincipal(usuarios.get(0));
        } else {
            escolherPerfil(usuarios);
        }
    }

    // Escolha de perfil sem senha (caso de uso "Fazer Login"), mais simples para o idoso.
    // Só aparece se os arquivos tiverem mais de um perfil salvo.
    private static void escolherPerfil(List<Usuario> usuarios) {
        String[] opcoes = new String[usuarios.size()];
        for (int i = 0; i < usuarios.size(); i++) {
            opcoes[i] = (i + 1) + " - " + usuarios.get(i).getNome();   // o número evita confundir nomes iguais
        }

        Object escolha = JOptionPane.showInputDialog(null, "Quem vai usar o MedLembre?", "MedLembre",
                JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[0]);
        if (escolha == null) {
            sair();
            return;
        }
        for (int i = 0; i < usuarios.size(); i++) {
            if (opcoes[i].equals(escolha)) {
                abrirPrincipal(usuarios.get(i));
                return;
            }
        }
    }

    private static void abrirPrincipal(Usuario usuario) {
        new TelaPrincipal(gerenciadorUsuario, gerenciadorRemedio, gerenciadorAlarme,
                usuario, App::abrirTelaInicial).exibir();
    }

    private static void sair() {
        System.exit(0);
    }
}
