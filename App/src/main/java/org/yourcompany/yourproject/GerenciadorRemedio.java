package org.yourcompany.yourproject;

import java.util.ArrayList;
import java.util.List;

public class GerenciadorRemedio {

    private List<Remedio> listaRemedios;
    private int proximoId;

    public GerenciadorRemedio() {
        this.listaRemedios = new ArrayList<>();
        this.proximoId = 1;
    }

    // Cadastra um remédio já montado (dá um id automático e salva no CSV)
    public void cadastrarRemedio(Remedio r) {
        r.setId(proximoId);
        proximoId++;
        listaRemedios.add(r);
        CsvUtil.salvarRemedios(listaRemedios);
    }

    // Monta um remédio líquido (dosagem em ml) e cadastra
    public void cadastrarRemedioLiquido(String nomeTecnico, double ml, int estoqueAtual) {
        Remedio r = new Remedio(nomeTecnico, ml, estoqueAtual);
        cadastrarRemedio(r);
    }

    // Monta um remédio em comprimidos e cadastra
    public void cadastrarRemedioComprimido(String nomeTecnico, int qteComp, int estoqueAtual) {
        Remedio r = new Remedio(nomeTecnico, qteComp, estoqueAtual);
        cadastrarRemedio(r);
    }

    // Devolve a lista com todos os remédios cadastrados
    public List<Remedio> listarRemedios() {
        return new ArrayList<>(listaRemedios);
    }

    // Tira "qtd" do estoque do remédio com esse id. Retorna true se deu certo.
    public boolean darBaixaEstoque(int idRemedio, int qtd) {
        Remedio r = buscarPorId(idRemedio);
        if (r == null || qtd <= 0 || r.getEstoqueAtual() < qtd) {
            return false;
        }
        r.darBaixaEstoque(qtd);
        CsvUtil.salvarRemedios(listaRemedios);
        return true;
    }

    // Devolve o remédio com esse id, ou null se não existir
    public Remedio buscarRemedio(int id) {
        return buscarPorId(id);
    }

    // Salva as alterações de um remédio já cadastrado (RF17)
    public boolean editarRemedio(Remedio r) {
        for (int i = 0; i < listaRemedios.size(); i++) {
            if (listaRemedios.get(i).getId() == r.getId()) {
                listaRemedios.set(i, r);
                CsvUtil.salvarRemedios(listaRemedios);
                return true;
            }
        }
        return false;
    }

    // Remove o remédio com esse id (RF16)
    public boolean deletarRemedio(int id) {
        Remedio r = buscarPorId(id);
        if (r == null) {
            return false;
        }
        listaRemedios.remove(r);
        CsvUtil.salvarRemedios(listaRemedios);
        return true;
    }

    // Soma "qtd" ao estoque, por exemplo ao comprar uma caixa nova (RF21)
    public boolean reporEstoque(int idRemedio, int qtd) {
        Remedio r = buscarPorId(idRemedio);
        if (r == null || qtd <= 0 || (long) r.getEstoqueAtual() + qtd > Integer.MAX_VALUE) {
            return false;   // quantidade inválida ou tão grande que estouraria o int
        }
        r.setEstoqueAtual(r.getEstoqueAtual() + qtd);
        CsvUtil.salvarRemedios(listaRemedios);
        return true;
    }

    // Apaga todos os remédios (usado quando o único perfil é excluído — RF08)
    public void limparDados() {
        listaRemedios.clear();
        CsvUtil.salvarRemedios(listaRemedios);
    }

    // Lê remedios.csv (a App chama uma vez, quando o programa abre)
    public void carregar() {
        listaRemedios = CsvUtil.carregarRemedios();
        proximoId = 1;
        for (Remedio r : listaRemedios) {
            if (r.getId() >= proximoId) {
                proximoId = r.getId() + 1;
            }
        }
    }

    // Procura um remédio pelo id (usado só aqui dentro)
    private Remedio buscarPorId(int id) {
        for (Remedio r : listaRemedios) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }
}