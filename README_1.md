# API REST de Produtos (Spring Boot)

API simples de cadastro de produtos, com os quatro verbos HTTP principais. Os dados ficam apenas em memória: somem quando a aplicação é encerrada.

## Estrutura

```
src/main/java/com/exemplo/produtos/
├── ApiProdutosApplication.java   classe principal (@SpringBootApplication)
├── Produto.java                  id (Integer), nome (String), preco (Double)
└── ProdutoController.java        @RestController com os 5 endpoints
src/test/java/com/exemplo/produtos/
└── ProdutoControllerTest.java    testes dos endpoints com MockMvc
```

## Endpoints

| Verbo | Rota | O que faz | Respostas |
|---|---|---|---|
| GET | `/produtos` | Lista todos os produtos | 200 |
| GET | `/produtos/{id}` | Busca um produto (`@PathVariable`) | 200, 404 |
| POST | `/produtos` | Cria um produto a partir do corpo (`@RequestBody`) | 201 + `Location`, 400 |
| PATCH | `/produtos/{id}` | Atualização parcial: só os campos enviados mudam | 200, 400, 404 |
| DELETE | `/produtos/{id}` | Remove o produto | 204, 404 |

Regras:
- O `id` é **gerado pela API**; um `id` enviado no corpo do POST ou do PATCH é ignorado
- `nome` é obrigatório no POST e não pode ser vazio; `preco` é obrigatório no POST e não pode ser negativo
- No PATCH, se qualquer campo for inválido, **nada** é alterado
- A aplicação já inicia com 3 produtos (ids 1 a 3)

## Como executar

Requer **JDK 17 ou superior** e **Maven 3.6.3 ou superior**.

```bash
mvn spring-boot:run     # sobe a API em http://localhost:8080
mvn test                # roda os testes
```

## Exemplos com curl

```bash
# listar
curl http://localhost:8080/produtos

# buscar o produto 1
curl http://localhost:8080/produtos/1

# criar
curl -i -X POST http://localhost:8080/produtos \
     -H "Content-Type: application/json" \
     -d '{"nome":"Monitor","preco":899.00}'

# atualizar só o preço do produto 4
curl -X PATCH http://localhost:8080/produtos/4 \
     -H "Content-Type: application/json" \
     -d '{"preco":750.00}'

# remover
curl -i -X DELETE http://localhost:8080/produtos/4
```

## Observações

- O PATCH foi escolhido (em vez de PUT) porque o enunciado pede atualização dos dados "de um produto existente" com `@RequestBody`; PATCH expressa melhor a atualização parcial
- A lista é um `CopyOnWriteArrayList` e o próximo id vem de um `AtomicInteger`, o que evita problemas se duas requisições chegarem ao mesmo tempo
- Os ids não são reaproveitados depois de uma remoção
