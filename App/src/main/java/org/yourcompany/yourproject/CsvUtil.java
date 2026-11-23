package org.yourcompany.yourproject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CsvUtil {

    // Pasta onde ficam os arquivos. Os testes da classe Certificadora usam outra pasta,
    // para nunca apagar os dados reais do programa.
    private static String pasta = "dados";

    // O que fazer quando um arquivo não puder ser lido ou gravado (a App mostra um aviso na tela)
    private static Consumer<String> aoFalhar;

    // Arquivos que não puderam ser gravados: o conteúdo mais recente fica guardado aqui e a
    // gravação é tentada de novo (a App chama regravarPendentes() de tempos em tempos)
    private static final Map<String, List<String>> pendentes = new LinkedHashMap<>();

    // Arquivos que não puderam ser lidos: não são regravados, para não apagar os dados que estão neles
    private static final Set<String> naoLidos = new HashSet<>();
    private static final Set<String> jaAvisados = new HashSet<>();

    public static void usarPasta(String novaPasta) {
        pasta = novaPasta;
    }

    public static String getPasta() {
        return pasta;
    }

    public static void setAoFalhar(Consumer<String> acao) {
        aoFalhar = acao;
    }

    // ---------- USUARIOS ----------
    public static void salvarUsuarios(List<Usuario> usuarios) {
        List<String> linhas = new ArrayList<>();
        for (Usuario u : usuarios) {
            linhas.add(u.getId() + ";" + u.getNome() + ";" + u.getDataNascimento() + ";" + u.getFotoPath() + ";" + u.getGeneroBiologico());
        }
        gravar("usuarios.csv", "id;nome;dataNascimento;fotoPath;generoBiologico", linhas);
    }
    // ---------- CONTATOS ----------
    public static void salvarContatos(List<Usuario> usuarios) {
        List<String> linhas = new ArrayList<>();
        for (Usuario u : usuarios) {
            for (ContatoEmergencia c : u.getContatos()) {
                linhas.add(c.getId() + ";" + u.getId() + ";" + c.getNome()+ ";" + c.getTelefone() + ";" + c.getRelacao());
            }
        }
        gravar("contatos.csv", "id;usuarioId;nome;telefone;relacao", linhas);
    }
    // ---------- REMEDIOS ----------
    public static void salvarRemedios(List<Remedio> remedios) {
        List<String> linhas = new ArrayList<>();
        for (Remedio r : remedios) {
            linhas.add(r.getId() + ";" + r.getNomeTecnico() + ";" + r.getDosagemEspecifica()+ ";" + r.getEstoqueAtual() + ";" + r.getEstoqueMinimo() + ";" + r.getTipo());
        }
        gravar("remedios.csv", "id;nomeTecnico;dosagemEspecifica;estoqueAtual;estoqueMinimo;tipo", linhas);
    }
    // ---------- LEMBRETES ----------
    public static void salvarLembretes(List<Lembrete> lembretes) {
        List<String> linhas = new ArrayList<>();
        for (Lembrete l : lembretes) {
            int remedioId = 0;
            if (l.getRemedio() != null) {
                remedioId = l.getRemedio().getId();
            }
            linhas.add(l.getId() + ";" + l.getHorario() + ";" + l.isAtivo()+ ";" + remedioId);
        }
        gravar("lembretes.csv", "id;horario;ativo;remedioId", linhas);
    }
    // ---------- HISTORICO ----------
    public static void salvarHistoricos(List<HistoricoMedicacao> historicos) {
        List<String> linhas = new ArrayList<>();
        for (HistoricoMedicacao h : historicos) {
            linhas.add(h.getId() + ";" + h.getDataHoraRegistro() + ";" + h.getStatus() + ";"
                    + h.getIdRemedio() + ";" + h.getNomeRemedio() + ";" + h.getHorarioPrevisto());
        }
        gravar("historicoMedicacao.csv", "id;dataHoraRegistro;status;idRemedio;nomeRemedio;horarioPrevisto", linhas);
    }
    // ==================== LEITURA (usada quando o programa abre) ====================

    // ---------- USUARIOS ----------
    public static List<Usuario> carregarUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        for (String[] col : lerLinhas("usuarios.csv")) {
            try {
                LocalDate nascimento = (texto(col[2]) == null) ? null : LocalDate.parse(col[2]);
                usuarios.add(new Usuario(Integer.parseInt(col[0]), col[1], nascimento, texto(col[3]), col[4]));
            } catch (RuntimeException e) {
                System.out.println("Linha inválida ignorada em usuarios.csv");
            }
        }
        return usuarios;
    }
    // ---------- CONTATOS (cada contato volta para dentro do seu usuário) ----------
    public static void carregarContatos(List<Usuario> usuarios) {
        for (String[] col : lerLinhas("contatos.csv")) {
            try {
                int usuarioId = Integer.parseInt(col[1]);
                for (Usuario u : usuarios) {
                    if (u.getId() == usuarioId) {
                        u.adicionarContato(new ContatoEmergencia(Integer.parseInt(col[0]), col[2], col[3], col[4]));
                    }
                }
            } catch (RuntimeException e) {
                System.out.println("Linha inválida ignorada em contatos.csv");
            }
        }
    }
    // ---------- REMEDIOS ----------
    public static List<Remedio> carregarRemedios() {
        List<Remedio> remedios = new ArrayList<>();
        for (String[] col : lerLinhas("remedios.csv")) {
            try {
                Remedio r = new Remedio(Integer.parseInt(col[0]), col[1], col[2],
                        Integer.parseInt(col[3]), Integer.parseInt(col[4]));
                if (col.length > 5 && texto(col[5]) != null) {   // arquivos antigos não tinham a coluna "tipo"
                    try {
                        r.setTipo(Remedio.TipoRemedio.valueOf(col[5]));
                    } catch (IllegalArgumentException e) {
                        // mantém o tipo deduzido pela dosagem
                    }
                }
                remedios.add(r);
            } catch (RuntimeException e) {
                System.out.println("Linha inválida ignorada em remedios.csv");
            }
        }
        return remedios;
    }
    // ---------- LEMBRETES (cada lembrete volta a apontar para o seu remédio) ----------
    public static List<Lembrete> carregarLembretes(List<Remedio> remedios) {
        List<Lembrete> lembretes = new ArrayList<>();
        for (String[] col : lerLinhas("lembretes.csv")) {
            try {
                int remedioId = Integer.parseInt(col[3]);
                Remedio remedio = null;
                for (Remedio r : remedios) {
                    if (r.getId() == remedioId) {
                        remedio = r;
                    }
                }
                if (remedio == null) {
                    // Sem o remédio, o lembrete tocaria todo dia sem que nenhuma tela pudesse apagá-lo
                    System.out.println("Lembrete " + col[0] + " ignorado: o remédio " + remedioId + " não existe mais");
                    continue;
                }
                lembretes.add(new Lembrete(Integer.parseInt(col[0]), LocalTime.parse(col[1]),
                        Boolean.parseBoolean(col[2]), remedio));
            } catch (RuntimeException e) {
                System.out.println("Linha inválida ignorada em lembretes.csv");
            }
        }
        return lembretes;
    }
    // ---------- HISTORICO ----------
    public static List<HistoricoMedicacao> carregarHistoricos() {
        List<HistoricoMedicacao> historicos = new ArrayList<>();
        for (String[] col : lerLinhas("historicoMedicacao.csv")) {
            try {
                HistoricoMedicacao h = new HistoricoMedicacao(Integer.parseInt(col[0]),
                        LocalDateTime.parse(col[1]), col[2]);
                if (col.length > 5) {   // arquivos antigos tinham só id, data/hora e status
                    h.setIdRemedio(Integer.parseInt(col[3]));
                    h.setNomeRemedio(texto(col[4]));
                    h.setHorarioPrevisto((texto(col[5]) == null) ? null : LocalTime.parse(col[5]));
                }
                historicos.add(h);
            } catch (RuntimeException e) {
                System.out.println("Linha inválida ignorada em historicoMedicacao.csv");
            }
        }
        return historicos;
    }

    // Lê um CSV da pasta de dados, pula o cabeçalho e devolve as colunas de cada linha
    private static List<String[]> lerLinhas(String nomeArquivo) {
        List<String[]> linhas = new ArrayList<>();
        Path arquivo = Paths.get(pasta, nomeArquivo);
        if (!Files.exists(arquivo)) {
            return linhas;   // primeira vez que o programa roda: ainda não há dados
        }
        String conteudo;
        try {
            conteudo = decodificar(Files.readAllBytes(arquivo));
        } catch (IOException e) {
            naoLidos.add(arquivo.toAbsolutePath().toString());
            avisarFalha("Não foi possível ler o arquivo " + nomeArquivo + " (" + e.getMessage() + ").\n\n"
                    + "Os dados desse arquivo não foram carregados, e ele não será alterado,\n"
                    + "para não perder informações. Feche o MedLembre e abra de novo.");
            return linhas;
        }
        String[] todas = conteudo.split("\\r?\\n");
        for (int i = 1; i < todas.length; i++) {   // a linha 0 é o cabeçalho
            if (!todas[i].isBlank()) {
                linhas.add(todas[i].split(";", -1));
            }
        }
        return linhas;
    }

    // Os arquivos são gravados em UTF-8. Se alguém abrir e salvar um deles no Excel (Windows),
    // ele pode voltar em Windows-1252: nesse caso é lido assim, sem perder os acentos.
    private static String decodificar(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }

    // Na gravação, um campo vazio vira o texto "null"; aqui ele volta a ser null
    private static String texto(String valor) {
        if (valor == null || valor.isEmpty() || valor.equals("null")) {
            return null;
        }
        return valor;
    }

    // ---------- Método auxiliar: grava o arquivo inteiro de forma segura ----------
    // Escreve primeiro num arquivo temporário e só depois troca pelo verdadeiro. Se der erro
    // no meio (disco cheio, arquivo aberto em outro programa), o arquivo antigo fica inteiro.
    private static void gravar(String nomeArquivo, String cabecalho, List<String> linhas) {
        List<String> conteudo = new ArrayList<>();
        conteudo.add(cabecalho);
        conteudo.addAll(linhas);
        String caminho = Paths.get(pasta, nomeArquivo).toAbsolutePath().toString();
        if (naoLidos.contains(caminho)) {
            if (jaAvisados.add(caminho)) {
                avisarFalha("O arquivo " + nomeArquivo + " não foi alterado, porque ele não pôde ser lido\n"
                        + "quando o programa abriu. Feche o MedLembre e abra de novo.");
            }
            return;
        }
        try {
            escrever(nomeArquivo, conteudo);
            pendentes.remove(nomeArquivo);
        } catch (IOException e) {
            boolean primeiraFalha = !pendentes.containsKey(nomeArquivo);
            pendentes.put(nomeArquivo, conteudo);
            if (primeiraFalha) {
                avisarFalha("Não foi possível salvar o arquivo " + nomeArquivo + " (" + e.getMessage() + ").\n\n"
                        + "A alteração continua no programa e será gravada sozinha assim que o arquivo for liberado.\n"
                        + "Se ele estiver aberto em outro programa (por exemplo, o Excel), feche esse programa.\n"
                        + "Não é preciso repetir a operação.");
            }
        }
    }

    // Tenta de novo gravar os arquivos que falharam. Devolve true se não sobrou nenhum pendente.
    public static boolean regravarPendentes() {
        for (String nomeArquivo : new ArrayList<>(pendentes.keySet())) {
            try {
                escrever(nomeArquivo, pendentes.get(nomeArquivo));
                pendentes.remove(nomeArquivo);
                System.out.println(nomeArquivo + " gravado na nova tentativa");
            } catch (IOException e) {
                // continua pendente; tenta de novo na próxima vez
            }
        }
        return pendentes.isEmpty();
    }

    private static void escrever(String nomeArquivo, List<String> conteudo) throws IOException {
        Path destino = Paths.get(pasta, nomeArquivo);
        Path temporario = Paths.get(pasta, nomeArquivo + ".tmp");
        Files.createDirectories(destino.getParent());
        Files.write(temporario, conteudo, StandardCharsets.UTF_8);
        try {
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void avisarFalha(String mensagem) {
        System.out.println(mensagem);
        if (aoFalhar != null) {
            aoFalhar.accept(mensagem);
        }
    }
}
