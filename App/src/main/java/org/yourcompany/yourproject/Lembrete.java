/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.yourcompany.yourproject;

/**
 *
 * @author rocha
 */

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;


public class Lembrete {
    private int id;
    private LocalTime horario;
    private boolean ativo;
    private Remedio remedio;

    public Lembrete() {
    }

   
    public Lembrete(int id, LocalTime horario, boolean ativo, Remedio remedio) {
        this.id = id;
        this.horario = horario;
        this.ativo = ativo;
        this.remedio = remedio;
    }

    // Por quantos minutos depois do horário o alarme ainda toca. Cobre o computador que
    // estava suspenso no minuto exato ou o programa aberto um pouco depois do horário.
    public static final int TOLERANCIA_MINUTOS = 30;

    public boolean isHorarioAtivo() {
        return isHorarioAtivo(LocalTime.now());
    }

    // Está na hora do remédio do minuto marcado até TOLERANCIA_MINUTOS depois
    public boolean isHorarioAtivo(LocalTime agora) {
        if (!ativo || horario == null) {
            return false;
        }
        return minutosDesdeOHorario(agora) < TOLERANCIA_MINUTOS;
    }

    // Minutos passados desde o horário do lembrete, de 0 a 1439 (passa pela meia-noite:
    // o lembrete das 23:50 visto às 00:10 está 20 minutos atrasado)
    public long minutosDesdeOHorario(LocalTime agora) {
        long minutos = Duration.between(horario.truncatedTo(ChronoUnit.MINUTES),
                agora.truncatedTo(ChronoUnit.MINUTES)).toMinutes();
        return (minutos < 0) ? minutos + 24 * 60 : minutos;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public void setHorario(LocalTime horario) {
        this.horario = horario;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Remedio getRemedio() {
        return remedio;
    }
    
    public void setRemedio(Remedio remedio) { 
        this.remedio = remedio; 
    }
}
