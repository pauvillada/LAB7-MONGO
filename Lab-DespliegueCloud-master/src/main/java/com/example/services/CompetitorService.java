/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.services;

import com.example.PersistenceManager;
import com.example.models.Competitor;
import com.example.models.CompetitorDTO;
import com.example.models.Product;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Mauricio
 */
@Path("/competitors")
@Produces(MediaType.APPLICATION_JSON)
public class CompetitorService {

    @PersistenceContext(unitName = "mongoPU")
    EntityManager entityManager;

    @PostConstruct
    public void init() {
        try {
            entityManager = PersistenceManager.getInstance().getEntityManagerFactory().createEntityManager();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * GET http://localhost:8082/competitors
     * Consulta SIN parametros: todos los competidores ordenados por apellido.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {

        Query q = entityManager.createQuery("select u from Competitor u order by u.surname ASC");
        List<Competitor> competitors = q.getResultList();
        return Response.status(200).header("Access-Control-Allow-Origin", "*").entity(competitors).build();
    }

    /**
     * POST http://localhost:8082/competitors/add
     * Crea un competidor con su vehiculo (embebido) y su producto (OBLIGATORIO).
     */
    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCompetitor(CompetitorDTO competitor) {

        JSONObject rta = new JSONObject();

        // Regla del laboratorio: cada competidor DEBE tener 1 producto asociado
        Product p = competitor.getProduct();
        if (p == null || p.getName() == null || p.getName().trim().isEmpty()) {
            entityManager.close();
            rta.put("error", "El competidor debe tener un producto asociado (product con name, brand y purchaseDate)");
            return Response.status(400).header("Access-Control-Allow-Origin", "*").entity(rta.toJSONString()).build();
        }

        Competitor c = new Competitor();
        c.setAddress(competitor.getAddress());
        c.setAge(competitor.getAge());
        c.setCellphone(competitor.getCellphone());
        c.setCity(competitor.getCity());
        c.setCountry(competitor.getCountry());
        c.setName(competitor.getName());
        c.setSurname(competitor.getSurname());
        c.setTelephone(competitor.getTelephone());
        c.setVehicle(competitor.getVehicle());

        try {
            entityManager.getTransaction().begin();
            // 1. Se guarda el producto en su propia coleccion (PRODUCT)
            entityManager.persist(p);
            // 2. Se asocia el producto al competidor y se guarda el competidor
            c.setProduct(p);
            entityManager.persist(c);
            entityManager.getTransaction().commit();
            entityManager.refresh(c);
            rta.put("competitor_id", c.getId());
            rta.put("product_id", p.getId());
        } catch (Throwable t) {
            t.printStackTrace();
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            c = null;
            rta.put("error", t.toString());
        } finally {
            entityManager.clear();
            entityManager.close();
        }
        return Response.status(200).header("Access-Control-Allow-Origin", "*").entity(rta.toJSONString()).build();
    }

    /**
     * GET http://localhost:8082/competitors/nombres-con-a
     * Devuelve los NOMBRES de los competidores que empiezan por la letra 'A'.
     */
    @GET
    @Path("/nombres-con-a")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNamesStartingWithA() {

        TypedQuery<Competitor> query = (TypedQuery<Competitor>) entityManager.createQuery(
                "SELECT c FROM Competitor c WHERE c.name LIKE 'A%' ORDER BY c.name ASC");
        List<Competitor> competitors = query.getResultList();

        JSONArray nombres = new JSONArray();
        for (Competitor c : competitors) {
            nombres.add(c.getName());
        }
        return Response.status(200).header("Access-Control-Allow-Origin", "*").entity(nombres.toJSONString()).build();
    }

    /**
     * GET http://localhost:8082/competitors/{name}
     * Consulta CON parametro: competidores que tengan ese nombre.
     */
    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCompetitorsByName(@PathParam("name") String name) {

        TypedQuery<Competitor> query = (TypedQuery<Competitor>) entityManager.createQuery(
                "SELECT c FROM Competitor c WHERE c.name = :name");
        List<Competitor> competitors = query.setParameter("name", name).getResultList();
        return Response.status(200).header("Access-Control-Allow-Origin", "*").entity(competitors).build();
    }

    @OPTIONS
    public Response cors(@javax.ws.rs.core.Context HttpHeaders requestHeaders) {
        return Response.status(200).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS").header("Access-Control-Allow-Headers", "AUTHORIZATION, content-type, accept").build();
    }

}