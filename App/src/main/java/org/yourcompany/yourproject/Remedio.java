/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.yourcompany.yourproject;

/**
 *
 * @author rocha
 */
public class Remedio {

   
    private int id;
    private String nomeTecnico;
    private String dosagemEspecifica;
    private int estoqueAtual;
    private int estoqueMinimo;
    private TipoRemedio tipo;   // COMPRIMIDO ou LIQUIDO (atributo do diagrama de classes)

    public enum TipoRemedio {
    LIQUIDO("ml"),
    COMPRIMIDO("comprimido(s)");

    private final String unidade;

    TipoRemedio(String unidade) {
        this.unidade = unidade;
    }

    public String getUnidade() {
        return unidade;
    }
}
    
    public Remedio() {
    }

   
    public Remedio(String nomeTecnico, double ml, int estoqueAtual) {
        this.nomeTecnico = nomeTecnico;
        this.dosagemEspecifica = ml + " ml";
        this.estoqueAtual = estoqueAtual;
        this.estoqueMinimo = 5;
        this.tipo = TipoRemedio.LIQUIDO;
    }

  
    public Remedio(String nomeTecnico, int qteComp, int estoqueAtual) {
        this.nomeTecnico = nomeTecnico;
        this.dosagemEspecifica = qteComp + " comprimido(s)";
        this.estoqueAtual = estoqueAtual;
        this.estoqueMinimo = 10;
        this.tipo = TipoRemedio.COMPRIMIDO;
    }

   
    public Remedio(int id, String nomeTecnico, String dosagemEspecifica, int estoqueAtual, int estoqueMinimo) {
        this.id = id;
        this.nomeTecnico = nomeTecnico;
        this.dosagemEspecifica = dosagemEspecifica;
        this.estoqueAtual = estoqueAtual;
        this.estoqueMinimo = estoqueMinimo;
        // Deduz o tipo pelo texto da dosagem (o CsvUtil depois confirma pela coluna "tipo")
        if (dosagemEspecifica != null && dosagemEspecifica.endsWith(" ml")) {
            this.tipo = TipoRemedio.LIQUIDO;
        } else {
            this.tipo = TipoRemedio.COMPRIMIDO;
        }
    }


    // Quantas unidades do estoque uma dose consome: para comprimidos, o número de
    // comprimidos da dose; para líquidos, 1 (o estoque de líquidos é contado em doses)
    public int getQuantidadePorDose() {
        if (tipo == TipoRemedio.COMPRIMIDO && dosagemEspecifica != null) {
            try {
                return Math.max(1, Integer.parseInt(dosagemEspecifica.trim().split(" ")[0]));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    public TipoRemedio getTipo() {
        return tipo;
    }

    public void setTipo(TipoRemedio tipo) {
        this.tipo = tipo;
    }

    public String getInstrucoesDosagem() {
        return "Tomar " + this.dosagemEspecifica + " de " + this.nomeTecnico;
    }

 
    public void darBaixaEstoque(int qtd) {
        if (this.estoqueAtual >= qtd) {
            this.estoqueAtual -= qtd;
        } else {
            System.out.println("Estoque insuficiente para o remédio: " + this.nomeTecnico);
        }
    }

   
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomeTecnico() {
        return nomeTecnico;
    }

    public void setNomeTecnico(String nomeTecnico) {
        this.nomeTecnico = nomeTecnico;
    }

    public String getDosagemEspecifica() {
        return dosagemEspecifica;
    }

    public void setDosagemEspecifica(String dosagemEspecifica) {
        this.dosagemEspecifica = dosagemEspecifica;
    }

    public int getEstoqueAtual() {
        return estoqueAtual;
    }

    public void setEstoqueAtual(int estoqueAtual) {
        this.estoqueAtual = estoqueAtual;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(int estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

}
