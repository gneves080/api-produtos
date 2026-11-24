package com.exemplo.produtos;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    // Armazenamento em memória: os dados somem quando a aplicação é encerrada
    private final List<Produto> produtos = new CopyOnWriteArrayList<>();
    private final AtomicInteger proximoId = new AtomicInteger(1);

    public ProdutoController() {
        // Alguns produtos iniciais, para a listagem já mostrar algo
        produtos.add(new Produto(proximoId.getAndIncrement(), "Notebook", 3500.00));
        produtos.add(new Produto(proximoId.getAndIncrement(), "Mouse", 59.90));
        produtos.add(new Produto(proximoId.getAndIncrement(), "Teclado", 149.90));
    }

    /** GET /produtos: lista todos os produtos. */
    @GetMapping
    public List<Produto> listar() {
        return new ArrayList<>(produtos);
    }

    /** GET /produtos/{id}: devolve um produto, ou 404 se não existir. */
    @GetMapping("/{id}")
    public Produto buscar(@PathVariable Integer id) {
        return procurar(id).orElseThrow(() -> naoEncontrado(id));
    }

    /** POST /produtos: cria um produto. O id é gerado pela API (um id enviado no corpo é ignorado). */
    @PostMapping
    public ResponseEntity<Produto> criar(@RequestBody Produto dados) {
        if (dados.getNome() == null || dados.getNome().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome é obrigatório.");
        }
        validarPreco(dados.getPreco(), true);

        Produto novo = new Produto(proximoId.getAndIncrement(), dados.getNome().trim(), dados.getPreco());
        produtos.add(novo);
        return ResponseEntity.created(URI.create("/produtos/" + novo.getId())).body(novo);
    }

    /**
     * PATCH /produtos/{id}: atualização parcial. Só os campos enviados no corpo são alterados;
     * os ausentes (ou nulos) permanecem como estavam. O id nunca muda.
     */
    @PatchMapping("/{id}")
    public Produto atualizar(@PathVariable Integer id, @RequestBody Produto dados) {
        Produto existente = procurar(id).orElseThrow(() -> naoEncontrado(id));

        // Valida tudo ANTES de alterar qualquer campo: se algo for inválido, nada é modificado
        if (dados.getNome() != null && dados.getNome().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome não pode ser vazio.");
        }
        validarPreco(dados.getPreco(), false);

        if (dados.getNome() != null) {
            existente.setNome(dados.getNome().trim());
        }
        if (dados.getPreco() != null) {
            existente.setPreco(dados.getPreco());
        }
        return existente;
    }

    /** DELETE /produtos/{id}: remove o produto. Responde 204 (sem corpo), ou 404 se não existir. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Integer id) {
        Produto existente = procurar(id).orElseThrow(() -> naoEncontrado(id));
        produtos.remove(existente);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- Auxiliares

    private Optional<Produto> procurar(Integer id) {
        return produtos.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    private ResponseStatusException naoEncontrado(Integer id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto " + id + " não encontrado.");
    }

    private void validarPreco(Double preco, boolean obrigatorio) {
        if (preco == null) {
            if (obrigatorio) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O preço é obrigatório.");
            }
            return;
        }
        if (preco < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O preço não pode ser negativo.");
        }
    }
}
