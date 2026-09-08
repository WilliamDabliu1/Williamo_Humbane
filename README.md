# Williamo_Humbane

## 1. Script de base de dados

Ficheiro: `scripts/database.sql`

A tabela `weather_query` guarda cada consulta de clima.

Para executar o script:

1. Arrancar a aplicação
2. Abrir http://localhost:8081/h2-console
3. JDBC URL: `jdbc:h2:file:C:/Teste Bim/Williamo_Humbane/demo/data/ditidb`
4. User: `sa` / Password: (vazio)
5. Colar e executar `scripts/schema.sql`


## 2. Tecnologias:
Linguagem :Java 17
Framework  :  Spring Boot 4.1.1
API Web  : Spring Web MVC, Implementação dos endpoints REST, Comunicação através de HTTP e JSON.

Persistência: Spring Data JPA
Cliente HTTP: RestTemplate 
Build e Gestão de Dependências : Maven 3.9.x /Empacotamento da aplicação em JAR.
Configuração: application.yml

## 3. Chamadas de endpoint
Clima

```http
GET /api/weather?city=Maputo
GET /api/weather?latitude=-25.953724&longitude=32.5892
GET /api/weather?city=Maputo&latitude=-25.953724&longitude=32.5892
```


curl "http://localhost:8081/api/weather?city=Maputo"
curl "http://localhost:8081/api/weather?latitude=-25.953724&longitude=32.5892"
curl "http://localhost:8081/api/weather?city=Maputo&latitude=-25.953724&longitude=32.5892"
```

### Histórico

http
GET /api/weather/history
GET /api/weather/history?page=0&size=10
GET /api/weather/history?city=Maputo&page=0&size=10
GET /api/weather/history?country=Mozambique
GET /api/weather/history?startDate=2026-07-01&endDate=2026-07-14
GET /api/weather/history/{id}
DELETE /api/weather/history/{id}
