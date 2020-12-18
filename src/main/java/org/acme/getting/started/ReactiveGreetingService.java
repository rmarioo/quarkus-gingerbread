package org.acme.getting.started;


import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import reactor.core.publisher.Mono;

@ApplicationScoped
public class ReactiveGreetingService {

    private static final Logger LOG = Logger.getLogger(ReactiveGreetingService.class);
    @Inject
    Vertx vertx;

    private WebClient client;

    @PostConstruct
    void initialize() {
        this.client = WebClient.create(vertx,
            new WebClientOptions()
                .setDefaultHost("localhost")
                .setDefaultPort(8087)
        .setConnectTimeout(50)
        .setIdleTimeout(80)
        );
    }


  public Uni<Gingerbread> prepareGingerbread() {

    Mono<Gingerbread> gingerbreadMono = buyMissingIngredients()
        .filter(succeeded -> succeeded)
        .flatMap(x -> heatButterWithHoney())
        .zipWith(prepareDough())
        .map(heatedAndDough -> mixDoughWithButter(heatedAndDough.getT1(), heatedAndDough.getT2()))
        .zipWith(prepareCakeTray())
        .zipWith(heatOven(), (contentAndVesselTuple, ovenHeated) ->
            bake(ovenHeated, contentAndVesselTuple.getT1(), contentAndVesselTuple.getT2()))
        .zipWith(prepareIcing(), (baked, icing) -> new Gingerbread(baked, icing));

    return Uni.createFrom().publisher(gingerbreadMono);

  }

  private Mono<Boolean> buyMissingIngredients() {
    return monoFrom(buyMissingIngredientsUni());
  }

  private static Mono<Boolean> monoFrom(Uni<Boolean> uni) {
    return uni.ifNoItem().after(Duration.ofMillis(100)).recoverWithItem(() -> false).
        convert().with(UniReactorConverters.toMono());
  }


  public Uni<String> endpoint(String name) {
        return client.get("/" + name)
            .send()
            .map(resp -> resp.bodyAsString()
            );
    }


    enum  MethodName {
        ingredients, heat, dough, tray, icing
    }

    enum  Ingredient {
        butter, honey, flour, soda, cinammon, sugar, egg
    }

    Set<Ingredient> existingIngredients = setOf(Ingredient.butter, Ingredient.honey, Ingredient.flour);
    Set<Ingredient> requiredIngredients = setOf(Ingredient.butter, Ingredient.honey, Ingredient.flour, Ingredient.soda, Ingredient.cinammon, Ingredient.sugar, Ingredient.egg);

    public Mono<Boolean> heatOven() {
        return Mono.just(true);
    }

    public Uni<Boolean> buyMissingIngredientsUni( ) {

      Set<Ingredient> ingredientsToBuy = ingredientsToBuy(requiredIngredients, existingIngredients);

      String ingredientToBuyString = ingredientsToBuy.stream().map(e -> e.name()).collect(Collectors.joining(","));

      return webClientCall(MethodName.ingredients, ingredientToBuyString).map( s-> {
          if (s.equals("ok") ) {
              return true;
          }
          else throw new RuntimeException("cannot get missing ingredients");
      });
  }


    private static <T> Set<T> setOf(T... values) {

        Stream<T> stream = Stream.of(values);
        HashSet<T> requiredIngredients = new HashSet<>();

        stream.forEach(requiredIngredients::add);
        return requiredIngredients;
    }

    private Set<Ingredient> ingredientsToBuy(Set<Ingredient> requiredIngredients, Set<Ingredient> existingIngredients) {
        Set<Ingredient> requiredIngredientsCloned = cloneSet(requiredIngredients);
        requiredIngredientsCloned.removeAll(existingIngredients);

        return requiredIngredientsCloned;
    }

    private Set<Ingredient> cloneSet(Set<Ingredient> requiredIngredients) {

      return new HashSet(requiredIngredients);
    }

    public Mono<Boolean> heatButterWithHoney() {
      return monoFrom(heatButterWithHoneyUni());
    }

    public Uni<Boolean> heatButterWithHoneyUni() {
        return webClientCall(MethodName.heat, "butter,honey").map( s-> s.equals("ok"));
    }

    public Mono<Boolean> prepareDough() {
      return monoFrom(prepareDoughUni());
    }

    public Uni<Boolean> prepareDoughUni() {
        return webClientCall(MethodName.dough, "flour,cinammon,soda,sugar").map( s-> s.equals("ok"));
    }

    public Mono<Boolean> prepareCakeTray() {
      return monoFrom(prepareCakeTrayUni());
    }

    public Uni<Boolean> prepareCakeTrayUni() {
        return webClientCall(MethodName.tray).map( s-> s.equals("ok"));
    }

   public Mono<Boolean> prepareIcing() {
      return monoFrom(prepareIcingUni());
   }
   public Uni<Boolean> prepareIcingUni() {
        return webClientCall(MethodName.icing).map( s-> s.equals("ok"));
    }

  protected boolean mixDoughWithButter(Boolean heatButter, Boolean dough ) {
      return heatButter && dough;
  }
  protected Boolean bake( Boolean ovenHeated,Boolean content ,Boolean vessel)  {
    return ovenHeated && content && vessel;
  }

    private Uni<String> webClientCall(MethodName methodName,String paramValue) {
      return endpoint(methodName.name()+ (paramValue != null ? "?value=" + paramValue : ""));
    }

  private Uni<String> webClientCall(MethodName methodName) {
 //   LOG.info("webClientCall calling " + methodName.name() + " with paramValue null" );

    return webClientCall(methodName,null);
  }

  public static class Gingerbread {

    private final Boolean baked;
    private final Boolean icing;

    public Gingerbread(Boolean baked, Boolean icing) {
      this.baked = baked;
      this.icing = icing;
    }

    public Boolean getBaked() {
      return baked;
    }

    public Boolean getIcing() {
      return icing;
    }
  }


}
