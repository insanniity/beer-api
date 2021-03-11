package one.digitalinnovation.beerstock.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BeerStockDoesNotContainException extends Exception {

    public BeerStockDoesNotContainException(Long id, int quantityToDecrement) {
        super(String.format("Beers with %s ID does not contains value to decrement: %s", id, quantityToDecrement));
    }
}
