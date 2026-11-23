package org.yourcompany.yourproject;

import java.awt.Toolkit;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Timer;

public class GerenciadorAlarme {

    private List<Lembrete> lembretes;
    private List<HistoricoMedicacao> historico;
    private Timer timer;
    private GerenciadorRemedio gerenciadorRemedio;

    // O que fazer quando um alarme toca (a App liga isso à TelaNotificacaoSwing)
    private Consumer<Lembrete> aoDisparar;

    private int proximoIdLembrete;
    private int proximoIdHistorico;

    // Guarda, para cada remédio + horário, o dia da última dose que já tocou (para tocar só
    // 1 vez por dia). Não usa o id do lembrete, porque ele muda quando o remédio é editado.
    private Map<String, LocalDate> ultimosDisparos;

    public GerenciadorAlarme(GerenciadorRemedio gerenciadorRemedio) {
        this.gerenciadorRemedio = gerenciadorRemedio;
        this.lembretes = new ArrayList<>();
        this.historico = new ArrayList<>();
        this.ultimosDisparos = new HashMap<>();
        this.proximoIdLembrete = 1;
        this.proximoIdHistorico = 1;
    }

    // ---------- Lembretes ----------

    // Cadastra um lembrete novo (id automático) e salva no CSV
    public void cadastrarLembrete(Lembrete l) {
        l.setId(proximoIdLembrete);
        proximoIdLembrete++;
        lembretes.add(l);
        // Um horário que já passou hoje não toca na hora do cadastro: vale a partir da próxima vez
        LocalDateTime agora = LocalDateTime.now();
        if (l.isHorarioAtivo(agora.toLocalTime()) && l.minutosDesdeOHorario(agora.toLocalTime()) > 0) {
            ultimosDisparos.put(chaveDisparo(l), dataDaDose(l.getHorario(), agora));
        }
        CsvUtil.salvarLembretes(lembretes);
    }

    public List<Lembrete> listarLembretes() {
        return new ArrayList<>(lembretes);
    }

    // Remove todos os lembretes de um remédio (ao editar os horários ou excluir o remédio)
    public void removerLembretesDoRemedio(int idRemedio) {
        lembretes.removeIf(l -> l.getRemedio() != null && l.getRemedio().getId() == idRemedio);
        CsvUtil.salvarLembretes(lembretes);
    }

    // Define o que acontece quando um alarme toca
    public void setAoDisparar(Consumer<Lembrete> aoDisparar) {
        this.aoDisparar = aoDisparar;
    }

    // Lê lembretes.csv e historicoMedicacao.csv (a App chama uma vez, quando o programa abre).
    // Precisa ser chamado DEPOIS do carregar() do GerenciadorRemedio.
    public void carregar() {
        lembretes = CsvUtil.carregarLembretes(gerenciadorRemedio.listarRemedios());
        historico = CsvUtil.carregarHistoricos();
        proximoIdLembrete = 1;
        for (Lembrete l : lembretes) {
            if (l.getId() >= proximoIdLembrete) {
                proximoIdLembrete = l.getId() + 1;
            }
        }
        proximoIdHistorico = 1;
        for (HistoricoMedicacao h : historico) {
            if (h.getId() >= proximoIdHistorico) {
                proximoIdHistorico = h.getId() + 1;
            }
        }
        // Doses já respondidas não voltam a tocar se o programa for reaberto logo depois
        ultimosDisparos.clear();
        for (HistoricoMedicacao h : historico) {
            if (h.getHorarioPrevisto() != null && h.getDataHoraRegistro() != null) {
                String chave = chaveDisparo(h.getIdRemedio(), h.getHorarioPrevisto());
                LocalDate dia = dataDaDose(h.getHorarioPrevisto(), h.getDataHoraRegistro());
                if (ultimosDisparos.get(chave) == null || dia.isAfter(ultimosDisparos.get(chave))) {
                    ultimosDisparos.put(chave, dia);
                }
            }
        }
    }

    // Apaga lembretes e histórico (usado quando o único perfil é excluído — RF08)
    public void limparDados() {
        lembretes.clear();
        historico.clear();
        ultimosDisparos.clear();
        CsvUtil.salvarLembretes(lembretes);
        CsvUtil.salvarHistoricos(historico);
    }

    // ---------- Monitoramento ----------

    // Liga o relógio: a cada 1 segundo, chama verificarLembretes()
    public void iniciarMonitoramento() {
        timer = new Timer(1000, e -> verificarLembretes());
        timer.start();
    }

    public void pararMonitoramento() {
        if (timer != null) {
            timer.stop();
        }
    }

    // Percorre os lembretes e dispara o alerta dos que estão na hora
    private void verificarLembretes() {
        LocalDateTime agora = LocalDateTime.now();   // uma só leitura do relógio (virada da meia-noite)
        for (Lembrete l : lembretes) {
            if (l.isHorarioAtivo(agora.toLocalTime())) {
                String chave = chaveDisparo(l);
                LocalDate diaDaDose = dataDaDose(l.getHorario(), agora);
                if (!diaDaDose.equals(ultimosDisparos.get(chave))) {
                    ultimosDisparos.put(chave, diaDaDose);
                    dispararAlerta(l);
                }
            }
        }
    }

    // Identifica "este remédio neste horário"
    private static String chaveDisparo(Lembrete l) {
        int idRemedio = (l.getRemedio() == null) ? 0 : l.getRemedio().getId();
        return chaveDisparo(idRemedio, l.getHorario());
    }

    private static String chaveDisparo(int idRemedio, LocalTime horario) {
        return idRemedio + "@" + horario.truncatedTo(ChronoUnit.MINUTES);
    }

    // Dia a que a dose pertence: o lembrete das 23:50 respondido às 00:05 é a dose de ontem
    public static LocalDate dataDaDose(LocalTime horario, LocalDateTime momento) {
        LocalDate dia = momento.toLocalDate();
        if (momento.toLocalTime().truncatedTo(ChronoUnit.MINUTES).isBefore(horario.truncatedTo(ChronoUnit.MINUTES))) {
            dia = dia.minusDays(1);
        }
        return dia;
    }

    // ---------- Alerta ----------

    // Mostra o aviso e toca o som (RF18)
    public void dispararAlerta(Lembrete l) {
        Toolkit.getDefaultToolkit().beep();
        if (l.getRemedio() != null) {
            System.out.println("ALERTA: " + l.getRemedio().getInstrucoesDosagem());
        } else {
            System.out.println("ALERTA: hora do remédio!");
        }
        // Abre a TelaNotificacaoSwing (quem faz essa ligação é a classe App)
        if (aoDisparar != null) {
            aoDisparar.accept(l);
        }
    }

    // O usuário tomou o remédio: registra "TOMADO" e tira do estoque a quantidade da dose
    public void confirmarIngestao(Lembrete l) {
        registrarHistorico("TOMADO", l);
        Remedio r = l.getRemedio();
        if (r != null) {
            // Se restar menos que a dose, o estoque vai a zero (nunca fica negativo)
            int quantidade = Math.min(r.getQuantidadePorDose(), r.getEstoqueAtual());
            if (quantidade > 0) {
                gerenciadorRemedio.darBaixaEstoque(r.getId(), quantidade);
            }
        }
    }

    // O usuário cancelou o alarme (RF19): registra "CANCELADO"
    public void cancelarAlerta(Lembrete l) {
        registrarHistorico("CANCELADO", l);
    }

    // ---------- Histórico ----------

    public List<HistoricoMedicacao> getHistorico() {
        return new ArrayList<>(historico);
    }

    // Texto com os últimos 7 dias (RF20)
    public String gerarRelatorioSemanal() {
        return HistoricoMedicacao.gerarRelatorioSemanal(historico);
    }

    // Cria um registro com o status recebido e o remédio/horário do lembrete, guarda e salva no CSV
    private void registrarHistorico(String status, Lembrete l) {
        HistoricoMedicacao h = new HistoricoMedicacao(proximoIdHistorico, status);
        proximoIdHistorico++;
        if (l != null && l.getHorario() != null) {
            h.setHorarioPrevisto(l.getHorario().truncatedTo(ChronoUnit.MINUTES));
        }
        if (l != null && l.getRemedio() != null) {
            h.setIdRemedio(l.getRemedio().getId());
            h.setNomeRemedio(l.getRemedio().getNomeTecnico());
        }
        historico.add(h);
        CsvUtil.salvarHistoricos(historico);
    }
}