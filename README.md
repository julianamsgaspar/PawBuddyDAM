# PawBuddy Mobile – Aplicação de Gestão de Adoção Animal

É uma aplicação móvel desenvolvida para a gestão de processos de adoção animal de uma instituição. A aplicação permite a consulta de animais disponíveis para adoção, bem como a submissão e acompanhamento de pedidos de adoção.

A solução inclui autenticação de utilizadores e diferenciação de permissões entre utilizadores comuns e administradores, garantindo uma gestão eficiente e organizada do sistema.

A aplicação móvel foi desenvolvida em integração com o backend previamente criado na unidade curricular de Desenvolvimento Web, permitindo a comunicação entre a aplicação Android e a base de dados central.


## Contexto Académico

Projeto desenvolvido no âmbito da unidade curricular de Desenvolvimento de Aplicações Moveis da Lcenciatura de Engenharia Informática, intregrada no 3º ano, 5 semestre. 

## Objetivos

- Facilitar o processo de adoção de animais
- Permitir a gestão digital de pedidos de adoção
 - Fornecer uma interface móvel intuitiva e acessível
- Integrar aplicação móvel com backend web existente
 - Garantir separação de permissões entre utilizadores e administradores

## Funcionalidades da Aplicação

 Utilizador
- Consulta de animais disponíveis para adoção
- Visualização detalhada de cada animal
- Autenticação (login)
- Submissão de pedidos de adoção
- Consulta das intenções de adoção
- Acompanhamento do estado dos pedidos

 Administrador
- Gestão de pedidos de adoção (visualizar, validar, alterar estado e eliminar)
- Gestão de animais (adicionar, editar, visualizar e eliminar)
- Gestão de utilizadores (visualizar e eliminar)
- Consulta e eliminação de adoções concluídas

## Arquitetura do Sistema

A aplicação segue uma arquitetura cliente-servidor:

Frontend Mobile: Android Studio (Kotlin)

Backend: API desenvolvida em projeto de Desenvolvimento Web (.Net)

A comunicação entre a aplicação móvel e o backend é feita através de requisições HTTP (API REST).

## Autores

- Juliana Mariana de Sousa Gaspar
- Inês Sapina Maciel
