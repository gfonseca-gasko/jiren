**Desenvolvimento**
Jiren usa o sistema de build do Maven, Kotlin com Springboot rodando na JVM 11 e Vaadin como framework front-end inbutido.

**Arquitetura**
A arquitetura não segue o padrão MVC do Spring e utiliza os serviços da AWS para autenticação e persistência (SecretManager e RDS).

**Build**
Para construir o app localmente utilize o profile "dev".

O profile "prod" precisar estar no ambiente da AWS para funcionar.

**UI**
A interface é integralmente construida utilizando o Vaadin Framework

**Objetivo**
O objetivo dessa prova de conceito é oferecer uma plataforma interna centralizada para gestão de comandos, monitoramento e automações em bancos de dados.
