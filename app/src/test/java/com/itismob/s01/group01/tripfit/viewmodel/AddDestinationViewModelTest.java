package com.itismob.s01.group01.tripfit.viewmodel;

import static org.junit.Assert.assertEquals;

import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.data.TripRepository;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class AddDestinationViewModelTest {

    private final List<Result.Callback<Void>> pending = new ArrayList<>();
    private int addCalls;
    private AddDestinationViewModel viewModel;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        TripRepository repository = (TripRepository) Proxy.newProxyInstance(
                TripRepository.class.getClassLoader(), new Class<?>[]{TripRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("addItineraryItem")) {
                        addCalls++;
                        pending.add((Result.Callback<Void>) args[args.length - 1]);
                    }
                    return null;
                });
        viewModel = new AddDestinationViewModel(repository);
    }

    @Test
    public void save_secondTapWhileInFlightIsIgnored() {
        viewModel.save("t", "Ueno", 1, null, false, null);
        viewModel.save("t", "Ueno", 1, null, false, null);

        assertEquals(1, addCalls);
    }

    @Test
    public void save_isAllowedAgainOnceTheFirstSaveFinished() {
        viewModel.save("t", "Ueno", 1, null, false, null);
        try {
            pending.get(0).onResult(Result.error("boom"));
        } catch (RuntimeException ignoredMainLooperStub) {
            // LiveData.setValue needs a main looper; the in-flight flag is cleared before it.
        }
        viewModel.save("t", "Ueno", 1, null, false, null);

        assertEquals(2, addCalls);
    }
}
