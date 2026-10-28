package com.example.duke.api.validation;

import com.example.duke.leaderboard.PlayerName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/// Defers to [PlayerName], so the rule is written once and the API and the domain can never
/// disagree about which names are acceptable.
public class PlayerNameValidator implements ConstraintValidator<ValidPlayerName, String> {

    @Override
    public boolean isValid(final String candidate, final ConstraintValidatorContext context) {
        return PlayerName.isAcceptable(candidate);
    }
}
