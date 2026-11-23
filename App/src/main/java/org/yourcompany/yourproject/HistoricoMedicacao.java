/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.yourcompany.yourproject;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoricoMedicacao {

    private int id;
    private LocalDateTime dataHoraRegistro;
    private String status;

    // Qual dose este registro representa (ligação com o Lembrete do diagrama de classes).
    // Guarda uma cópia do nome e do horário: o histórico continua legível mesmo se o
    // remédio for editado ou excluído depois.
    private int idRemedio;
    private String nomeRemedio;
    private LocalTime horarioPrevisto;

    public HistoricoMedicacao() {
    }

    // Usado ao criar um registro novo (a data e hora são preenchidas na hora)
    public HistoricoMedicacao(int id, String status) {
        this.id = id;
        this.status = status;
        registrar();
    }

    // Usado ao carregar do CSV (a data e hora já existem)
    public HistoricoMedicacao(int id, LocalDateTime dataHoraRegistro, String status) {
        this.id = id;
        this.dataHoraRegistro = dataHoraRegistro;
        this.status = status;
    }

    // Marca o registro com a data e hora de agora
    public void registrar() {
        this.dataHoraRegistro = LocalDateTime.now();
    }

    // Monta um texto com os registros dos últimos 7 dias
    public static String gerarRelatorioSemanal(List<HistoricoMedicacao> historicos) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        LocalDateTime limite = LocalDateTime.now().minusDays(7);

        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Histórico dos últimos 7 dias\n");

        int tomados = 0;
        int cancelados = 0;

        for (HistoricoMedicacao h : historicos) {
            if (h.getDataHoraRegistro().isAfter(limite)) {
                relatorio.append(h.getDataHoraRegistro().format(formato))
                         .append(" - ")
                         .append(h.getStatus());
                if (h.getNomeRemedio() != null) {
                    relatorio.append(" - ").append(h.getNomeRemedio());
                }
                relatorio.append("\n");

                if (h.getStatus().equals("TOMADO")) {
                    tomados++;
                } else if (h.getStatus().equals("CANCELADO")) {
                    cancelados++;
                }
            }
        }

        relatorio.append("Total tomados: ").append(tomados).append("\n");
        relatorio.append("Total cancelados: ").append(cancelados).append("\n");
        return relatorio.toString();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDataHoraRegistro() {
        return dataHoraRegistro;
    }

    public void setDataHoraRegistro(LocalDateTime dataHoraRegistro) {
        this.dataHoraRegistro = dataHoraRegistro;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getIdRemedio() {
        return idRemedio;
    }

    public void setIdRemedio(int idRemedio) {
        this.idRemedio = idRemedio;
    }

    public String getNomeRemedio() {
        return nomeRemedio;
    }

    public void setNomeRemedio(String nomeRemedio) {
        this.nomeRemedio = nomeRemedio;
    }

    public LocalTime getHorarioPrevisto() {
        return horarioPrevisto;
    }

    public void setHorarioPrevisto(LocalTime horarioPrevisto) {
        this.horarioPrevisto = horarioPrevisto;
    }
}
