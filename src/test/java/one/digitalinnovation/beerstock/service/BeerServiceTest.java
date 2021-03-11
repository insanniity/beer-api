package one.digitalinnovation.beerstock.service;

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder;
import one.digitalinnovation.beerstock.dto.BeerDTO;
import one.digitalinnovation.beerstock.entity.Beer;
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException;
import one.digitalinnovation.beerstock.exception.BeerNotFoundException;
import one.digitalinnovation.beerstock.exception.BeerStockDoesNotContainException;
import one.digitalinnovation.beerstock.exception.BeerStockExceededException;
import one.digitalinnovation.beerstock.mapper.BeerMapper;
import one.digitalinnovation.beerstock.repository.BeerRepository;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BeerServiceTest {

    private static final long INVALID_BEER_ID = 1L;

    @Mock
    private BeerRepository beerRepository;

    private BeerMapper beerMapper = BeerMapper.INSTANCE;

    @InjectMocks
    private BeerService beerService;

    private BeerDTO beerExpectedDTO = BeerDTOBuilder.builder().build().toBeerDTO();

    @Test
    void whenBeerInformedThenItShouldBeCreated() throws BeerAlreadyRegisteredException {
        Beer expectedSavedBeer = beerMapper.toModel(beerExpectedDTO);
        when(beerRepository.findByName(beerExpectedDTO.getName())).thenReturn(Optional.empty());
        when(beerRepository.save(expectedSavedBeer)).thenReturn(expectedSavedBeer);
        BeerDTO createdBeerDTO = beerService.createBeer(beerExpectedDTO);
        assertThat(createdBeerDTO.getId(), is(equalTo(beerExpectedDTO.getId())));
        assertThat(createdBeerDTO.getName(), is(equalTo(beerExpectedDTO.getName())));
        assertThat(createdBeerDTO.getQuantity(), is(equalTo(beerExpectedDTO.getQuantity())));
        assertThat(createdBeerDTO.getQuantity(), is(greaterThan(2)));

    }

    @Test
    void whenAlreadyRegisteredBeerInformedThenAnExceptionShouldBeThrown() {
        Beer duplicatedBeer = beerMapper.toModel(beerExpectedDTO);
        when(beerRepository.findByName(beerExpectedDTO.getName())).thenReturn(Optional.of(duplicatedBeer));
        assertThrows(BeerAlreadyRegisteredException.class, () -> beerService.createBeer(beerExpectedDTO));
    }

    @Test
    void whenValidBeerNameIsGivenThenReturnABeer() throws BeerNotFoundException {
        Beer expectedFoundBeer = beerMapper.toModel(beerExpectedDTO);
        when(beerRepository.findByName(expectedFoundBeer.getName())).thenReturn(Optional.of(expectedFoundBeer));
        BeerDTO foundBeerDTO = beerService.findByName(beerExpectedDTO.getName());
        assertThat(foundBeerDTO, is(equalTo(beerExpectedDTO)));
    }

    @Test
    void whenNotRegisteredBeerNameIsGivenThenThrowAnException() {
        when(beerRepository.findByName(beerExpectedDTO.getName())).thenReturn(Optional.empty());
        assertThrows(BeerNotFoundException.class, () -> beerService.findByName(beerExpectedDTO.getName()));
    }

    @Test
    void whenListBeerIsCalledThenReturnAListOfBeers() {
        Beer expectedFoundBeer = beerMapper.toModel(beerExpectedDTO);
        when(beerRepository.findAll()).thenReturn(Collections.singletonList(expectedFoundBeer));
        List<BeerDTO> foundListBeersDTO = beerService.listAll();
        assertThat(foundListBeersDTO, is(not(empty())));
        assertThat(foundListBeersDTO.get(0), is(equalTo(beerExpectedDTO)));
    }

    @Test
    void whenListBeerIsCalledThenReturnAnEmptyListOfBeers() {
        when(beerRepository.findAll()).thenReturn(Collections.EMPTY_LIST);
        List<BeerDTO> foundListBeersDTO = beerService.listAll();
        assertThat(foundListBeersDTO, is(empty()));
    }

    @Test
    void whenExclusionIsCalledWithValidIdThenABeerShouldBeDeleted() throws BeerNotFoundException{
        Beer expectedDeletedBeer = beerMapper.toModel(beerExpectedDTO);
        when(beerRepository.findById(beerExpectedDTO.getId())).thenReturn(Optional.of(expectedDeletedBeer));
        doNothing().when(beerRepository).deleteById(beerExpectedDTO.getId());
        beerService.deleteById(beerExpectedDTO.getId());
        verify(beerRepository, times(1)).findById(beerExpectedDTO.getId());
        verify(beerRepository, times(1)).deleteById(beerExpectedDTO.getId());
    }

    @Test
    void whenIncrementIsCalledThenIncrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        Beer expectedBeer = beerMapper.toModel(beerExpectedDTO);

        when(beerRepository.findById(beerExpectedDTO.getId())).thenReturn(Optional.of(expectedBeer));
        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);

        int quantityToIncrement = 10;
        int expectedQuantityAfterIncrement = beerExpectedDTO.getQuantity() + quantityToIncrement;


        BeerDTO incrementedBeerDTO = beerService.increment(beerExpectedDTO.getId(), quantityToIncrement);

        assertThat(expectedQuantityAfterIncrement, equalTo(incrementedBeerDTO.getQuantity()));
        assertThat(expectedQuantityAfterIncrement, lessThan(beerExpectedDTO.getMax()));
    }

    @Test
    void whenIncrementIsGreatherThanMaxThenThrowException() {
        Beer expectedBeer = beerMapper.toModel(beerExpectedDTO);

        when(beerRepository.findById(beerExpectedDTO.getId())).thenReturn(Optional.of(expectedBeer));

        int quantityToIncrement = 80;
        assertThrows(BeerStockExceededException.class, () -> beerService.increment(beerExpectedDTO.getId(), quantityToIncrement));
    }

    @Test
    void whenIncrementAfterSumIsGreatherThanMaxThenThrowException() {
        Beer expectedBeer = beerMapper.toModel(beerExpectedDTO);

        when(beerRepository.findById(beerExpectedDTO.getId())).thenReturn(Optional.of(expectedBeer));

        int quantityToIncrement = 45;
        assertThrows(BeerStockExceededException.class, () -> beerService.increment(beerExpectedDTO.getId(), quantityToIncrement));
    }

    @Test
    void whenIncrementIsCalledWithInvalidIdThenThrowException() {
        int quantityToIncrement = 10;

        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());

        assertThrows(BeerNotFoundException.class, () -> beerService.increment(INVALID_BEER_ID, quantityToIncrement));
    }


    @Test
    void whenDecrementIsCalledThenDecrementBeerStock() throws BeerNotFoundException, BeerStockDoesNotContainException {
        Beer expectedBeer = beerMapper.toModel(beerExpectedDTO);
        when(beerRepository.findById(beerExpectedDTO.getId())).thenReturn(Optional.of(expectedBeer));
        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);

        int quantityToDecrement = 5;
        int expectedQuantityAfterDecrement = beerExpectedDTO.getQuantity() - quantityToDecrement;
        BeerDTO incrementedBeerDTO = beerService.decrement(beerExpectedDTO.getId(), quantityToDecrement);

        assertThat(expectedQuantityAfterDecrement, equalTo(incrementedBeerDTO.getQuantity()));
        assertThat(expectedQuantityAfterDecrement, greaterThan(0));
    }

    @Test
    void whenDecrementIsCalledToEmptyStockThenEmptyBeerStock() throws BeerNotFoundException, BeerStockDoesNotContainException {
        Beer expectedBeer = beerMapper.toModel(beerExpectedDTO);

        when(beerRepository.findById(beerExpectedDTO.getId())).thenReturn(Optional.of(expectedBeer));
        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);

        int quantityToDecrement = 10;
        int expectedQuantityAfterDecrement = beerExpectedDTO.getQuantity() - quantityToDecrement;
        BeerDTO decrementedBeerDTO = beerService.decrement(beerExpectedDTO.getId(), quantityToDecrement);

        assertThat(expectedQuantityAfterDecrement, equalTo(0));
        assertThat(expectedQuantityAfterDecrement, equalTo(decrementedBeerDTO.getQuantity()));
    }

    @Test
    void whenDecrementIsLowerThanZeroThenThrowException() {
        Beer expectedBeer = beerMapper.toModel(beerExpectedDTO);

        when(beerRepository.findById(beerExpectedDTO.getId())).thenReturn(Optional.of(expectedBeer));

        int quantityToDecrement = 80;
        assertThrows(BeerStockDoesNotContainException.class, () -> beerService.decrement(beerExpectedDTO.getId(), quantityToDecrement));
    }

    @Test
    void whenDecrementIsCalledWithInvalidIdThenThrowException() {
        int quantityToDecrement = 10;

        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());

        assertThrows(BeerNotFoundException.class, () -> beerService.decrement(INVALID_BEER_ID, quantityToDecrement));
    }


}
