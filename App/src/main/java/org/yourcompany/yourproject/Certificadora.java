/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package org.yourcompany.yourproject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.io.File;

public class Certificadora {

    private static int ok = 0;
    private static int erros = 0;

    // Compara o resultado com o esperado e mostra OK ou ERRO na tela
    private static void verificar(String teste, boolean condicao) {
        if (condicao) {
            System.out.println("[OK]   " + teste);
            ok++;
        } else {
            System.out.println("[ERRO] " + teste);
            erros++;
        }
    }

    public static void main(String[] args) throws InterruptedException {

        // Os testes gravam numa pasta própria, para não apagar os dados reais do programa (pasta "dados")
        CsvUtil.usarPasta("dados-teste");

        GerenciadorUsuario gu = new GerenciadorUsuario();
        GerenciadorRemedio gr = new GerenciadorRemedio();
        GerenciadorAlarme ga = new GerenciadorAlarme(gr);

        // ==================== 1. USUARIO ====================
        System.out.println("\n--- TESTE 1: USUARIO ---");

        Usuario maria = new Usuario();
        maria.setNome("Maria");
        maria.setDataNascimento(LocalDate.of(1950, 5, 20));
        maria.setGeneroBiologico("Feminino");
        maria.setFotoPath("fotos/maria.png");

        verificar("Cadastrar usuário válido", gu.cadastrarUsuario(maria));
        verificar("Usuário recebeu id 1", maria.getId() == 1);

        Usuario semNome = new Usuario();
        semNome.setDataNascimento(LocalDate.of(1950, 1, 1));
        semNome.setGeneroBiologico("Masculino");
        verificar("Recusar usuário sem nome", !gu.cadastrarUsuario(semNome));

        verificar("Buscar usuário pelo id", gu.buscarUsuario(1) != null);
        verificar("Buscar id inexistente devolve null", gu.buscarUsuario(99) == null);

        maria.setNome("Maria Silva");
        verificar("Editar usuário", gu.editarUsuario(maria));
        verificar("Nome ficou alterado", gu.buscarUsuario(1).getNome().equals("Maria Silva"));

        // ==================== 2. CONTATOS ====================
        System.out.println("\n--- TESTE 2: CONTATOS DE EMERGÊNCIA ---");

        verificar("Adicionar contato 1", maria.adicionarContato(
                new ContatoEmergencia(1, "Ana", "(43) 99999-0001", "Filha")));
        verificar("Adicionar contato 2", maria.adicionarContato(
                new ContatoEmergencia(2, "Pedro", "(43) 99999-0002", "Filho")));
        verificar("Adicionar contato 3", maria.adicionarContato(
                new ContatoEmergencia(3, "Carlos", "(43) 99999-0003", "Vizinho")));
        verificar("Recusar o 4º contato (limite de 3)", !maria.adicionarContato(
                new ContatoEmergencia(4, "Extra", "(43) 99999-0004", "Amigo")));
        verificar("Usuário tem 3 contatos", maria.getContatos().size() == 3);

        maria.getContatos().get(0).enviarNotificacao();   // mensagem simulada

        maria.removerContato(3);
        verificar("Remover contato", maria.getContatos().size() == 2);

        gu.editarUsuario(maria);   // salva os contatos no CSV

        // ==================== 3. REMEDIOS ====================
        System.out.println("\n--- TESTE 3: REMEDIOS ---");

        gr.cadastrarRemedioComprimido("Losartana", 1, 30);
        gr.cadastrarRemedioLiquido("Xarope", 5.0, 20);   // com ponto decimal!

        verificar("Dois remédios cadastrados", gr.listarRemedios().size() == 2);

        Remedio losartana = gr.listarRemedios().get(0);
        Remedio xarope = gr.listarRemedios().get(1);

        verificar("Texto do comprimido", losartana.getInstrucoesDosagem()
                .equals("Tomar 1 comprimido(s) de Losartana"));
        verificar("Texto do líquido", xarope.getInstrucoesDosagem()
                .equals("Tomar 5.0 ml de Xarope"));
        System.out.println("       " + losartana.getInstrucoesDosagem());
        System.out.println("       " + xarope.getInstrucoesDosagem());

        verificar("Baixa de estoque válida", gr.darBaixaEstoque(1, 5));
        verificar("Estoque foi de 30 para 25", losartana.getEstoqueAtual() == 25);
        verificar("Recusar baixa maior que o estoque", !gr.darBaixaEstoque(1, 1000));
        verificar("Recusar baixa negativa", !gr.darBaixaEstoque(1, -3));
        verificar("Recusar baixa de remédio inexistente", !gr.darBaixaEstoque(99, 1));
        verificar("Estoque continuou 25", losartana.getEstoqueAtual() == 25);

        // ==================== 4. ALARME ====================
        System.out.println("\n--- TESTE 4: ALARME ---");

        // Lembrete com o horário de AGORA, para tocar no primeiro segundo
        Lembrete lembrete = new Lembrete(0, LocalTime.now(), true, losartana);
        ga.cadastrarLembrete(lembrete);
        verificar("Lembrete recebeu id 1", lembrete.getId() == 1);
        verificar("isHorarioAtivo() é true no horário atual", lembrete.isHorarioAtivo());

        Lembrete desligado = new Lembrete(0, LocalTime.now(), false, xarope);
        verificar("Lembrete desativado nunca dispara", !desligado.isHorarioAtivo());

        System.out.println("       Ligando o monitoramento por 3 segundos...");
        System.out.println("       (deve aparecer UM alerta e tocar um beep)");
        ga.iniciarMonitoramento();
        Thread.sleep(3000);
        ga.pararMonitoramento();

        ga.confirmarIngestao(lembrete);
        verificar("Confirmar ingestão baixou 1 do estoque", losartana.getEstoqueAtual() == 24);

        ga.cancelarAlerta(lembrete);
        verificar("Histórico tem 2 registros", ga.getHistorico().size() == 2);
        verificar("1º registro é TOMADO", ga.getHistorico().get(0).getStatus().equals("TOMADO"));
        verificar("2º registro é CANCELADO", ga.getHistorico().get(1).getStatus().equals("CANCELADO"));

        System.out.println("\n" + ga.gerarRelatorioSemanal());

        // ==================== 5. ARQUIVOS CSV ====================
        System.out.println("--- TESTE 5: ARQUIVOS CSV ---");

        String[] arquivos = {"usuarios.csv", "contatos.csv", "remedios.csv",
                             "lembretes.csv", "historicoMedicacao.csv"};
        for (String nome : arquivos) {
            verificar("Arquivo " + CsvUtil.getPasta() + "/" + nome + " existe", new File(CsvUtil.getPasta(), nome).exists());
        }

        // ==================== 6. DELETAR ====================
        System.out.println("\n--- TESTE 6: DELETAR USUARIO ---");

        Usuario joao = new Usuario();
        joao.setNome("João");
        joao.setDataNascimento(LocalDate.of(1948, 3, 10));
        joao.setGeneroBiologico("Masculino");
        gu.cadastrarUsuario(joao);

        verificar("João cadastrado (2 usuários na lista)", gu.listarUsuarios().size() == 2);
        verificar("Deletar usuário", gu.deletarUsuario(joao.getId()));
        verificar("João não existe mais", gu.buscarUsuario(joao.getId()) == null);
        verificar("Deletar id inexistente devolve false", !gu.deletarUsuario(99));

        // ==================== 7. CARREGAR DO DISCO ====================
        System.out.println("\n--- TESTE 7: CARREGAR DO DISCO (como ao reabrir o programa) ---");

        GerenciadorUsuario gu2 = new GerenciadorUsuario();
        GerenciadorRemedio gr2 = new GerenciadorRemedio();
        GerenciadorAlarme ga2 = new GerenciadorAlarme(gr2);
        gu2.carregar();
        gr2.carregar();
        ga2.carregar();   // depois dos remédios: cada lembrete aponta para um remédio

        verificar("Usuário voltou do arquivo", gu2.buscarUsuario(1) != null
                && gu2.buscarUsuario(1).getNome().equals("Maria Silva"));
        verificar("Contatos voltaram junto com o usuário", gu2.buscarUsuario(1).getContatos().size() == 2);
        verificar("Remédios voltaram com o tipo certo",
                gr2.buscarRemedio(1).getTipo() == Remedio.TipoRemedio.COMPRIMIDO
                && gr2.buscarRemedio(2).getTipo() == Remedio.TipoRemedio.LIQUIDO);
        verificar("Estoque salvo voltou (24)", gr2.buscarRemedio(1).getEstoqueAtual() == 24);
        verificar("Lembrete aponta para o remédio carregado",
                ga2.listarLembretes().get(0).getRemedio() == gr2.buscarRemedio(1));
        verificar("Histórico guarda o nome do remédio", "Losartana".equals(ga2.getHistorico().get(0).getNomeRemedio()));

        // ==================== 8. DOSE E HORÁRIO DO ALARME ====================
        System.out.println("\n--- TESTE 8: BAIXA PELA DOSE E HORÁRIO DO ALARME ---");

        Remedio dipirona = new Remedio("Dipirona", 2, 10);
        gr2.cadastrarRemedio(dipirona);
        Lembrete lembreteDipirona = new Lembrete(0, LocalTime.of(8, 0), true, dipirona);
        ga2.cadastrarLembrete(lembreteDipirona);
        ga2.confirmarIngestao(lembreteDipirona);
        verificar("Dose de 2 comprimidos baixa 2 do estoque (10 -> 8)", dipirona.getEstoqueAtual() == 8);
        verificar("Alarme vale no horário marcado", lembreteDipirona.isHorarioAtivo(LocalTime.of(8, 0)));
        verificar("Alarme ainda vale 29 min depois (computador suspenso)", lembreteDipirona.isHorarioAtivo(LocalTime.of(8, 29)));
        verificar("Alarme não vale 30 min depois", !lembreteDipirona.isHorarioAtivo(LocalTime.of(8, 30)));
        verificar("Alarme não vale antes do horário", !lembreteDipirona.isHorarioAtivo(LocalTime.of(7, 59)));
        Lembrete lembreteNoite = new Lembrete(0, LocalTime.of(23, 50), true, dipirona);
        verificar("Alarme das 23:50 ainda vale às 00:10 (virada do dia)", lembreteNoite.isHorarioAtivo(LocalTime.of(0, 10)));

        // ==================== RESULTADO ====================
        System.out.println("\n=============================");
        System.out.println("Testes OK:   " + ok);
        System.out.println("Testes ERRO: " + erros);
        System.out.println("=============================");

        System.exit(0);
    }
}