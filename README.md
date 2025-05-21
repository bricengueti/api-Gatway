# 🛡️ api-gateway

`api-gateway` est le point d’entrée unique de l’architecture microservices de ce projet. Il agit comme un **reverse proxy intelligent** permettant de centraliser la gestion des requêtes HTTP, la sécurité, et le routage vers les services internes.

---

## 🎯 Objectifs

- Centraliser les accès aux différents microservices
- Appliquer des règles de sécurité avant d’atteindre les services métiers
- Gérer les filtres d’authentification avec JWT
- Simplifier la configuration côté client grâce à un point d’entrée unique

---

## 🔐 Authentification avec JWT

Ce projet intègre un filtre personnalisé `JwtAuthenticationFilter` (ou `AuthenticationFilter`) qui :
- Intercepte chaque requête entrante
- Valide le token JWT signé avec une **clé RSA publique**
- Extrait les **claims du token** (ex: `userId`, `roles`, etc.)
- Rejette les requêtes non autorisées (erreurs 401 / 403)
- Ajoute les informations d'identité au contexte de sécurité

Le token JWT est émis par le **Auth-Service**, et la clé publique est utilisée ici pour vérifier sa validité.

---

## ⚙️ Technologies utilisées

- **Java 17**
- **Spring Cloud Gateway**
- **Spring Boot 3.x**
- **Spring Security**
- **JWT (RSA)**
- **Maven**

---

## 🚦 Routage des services

Le `application.yml` définit les routes exposées publiquement ou sécurisées. Exemple :

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://localhost:8070
          predicates:
            - Path=/api/auth/**

        - id: product-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/products/**
          filters:
            - AuthenticationFilter
