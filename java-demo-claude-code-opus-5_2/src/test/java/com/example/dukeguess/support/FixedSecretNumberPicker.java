package com.example.dukeguess.support;

import java.util.concurrent.atomic.AtomicInteger;

import com.example.dukeguess.game.GameRules;
import com.example.dukeguess.game.SecretNumberPicker;

/// Hands out a secret number the test chose, so a run can drive a round to a win or a
/// loss on purpose.
public class FixedSecretNumberPicker implements SecretNumberPicker {

    private final AtomicInteger secretNumber = new AtomicInteger(GameRules.LOWEST_NUMBER);

    public void willPick(final int next) {
        secretNumber.set(next);
    }

    @Override
    public int pick() {
        return secretNumber.get();
    }
}
