package com.ber.nimblePattern.probability;

import java.math.BigDecimal;
import java.math.BigInteger;

/** Exact arithmetic: rounding a probability changes the recipe's material balance. */
public record ExpectedAmount(BigInteger numerator, BigInteger denominator) {
    public ExpectedAmount {
        if (numerator.signum() < 0 || denominator.signum() <= 0) throw new IllegalArgumentException();
        var gcd = numerator.gcd(denominator);
        numerator = numerator.divide(gcd);
        denominator = denominator.divide(gcd);
    }

    public static ExpectedAmount of(long n, long d) {
        return new ExpectedAmount(BigInteger.valueOf(n), BigInteger.valueOf(d));
    }

    public static ExpectedAmount decimal(Number number) {
        var value = new BigDecimal(number.toString()).stripTrailingZeros();
        return new ExpectedAmount(value.unscaledValue().multiply(BigInteger.TEN.pow(Math.max(0, -value.scale()))),
                BigInteger.TEN.pow(Math.max(0, value.scale())));
    }

    public ExpectedAmount add(ExpectedAmount other) {
        return new ExpectedAmount(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    public ExpectedAmount multiply(long amount) {
        return new ExpectedAmount(numerator.multiply(BigInteger.valueOf(amount)), denominator);
    }

    public ExpectedAmount divide(ExpectedAmount other) {
        return new ExpectedAmount(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
    }

    public long scaled(long scale) {
        var value = numerator.multiply(BigInteger.valueOf(scale)).divideAndRemainder(denominator);
        if (value[1].signum() != 0) throw new ArithmeticException("Fractional output");
        return value[0].longValueExact();
    }
}
