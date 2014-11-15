base <- c(1, 1.2, 1.5, 2, 2.5, 3, 4, 5, 6, 7.5)
vals <- c(base, base*10, base*100, base*1000)
n <- length(vals)

# multipliers
mult <- vals / 75
plot(log(1:n-10)**2)

centered <- 1 / ((abs(1:n-21))**2+20)
cost_adjustment <- (1:n)**1.30
probs <- 1/vals * cost_adjustment * centered
probs <- probs/sum(probs)

costs <- probs * mult
plot(costs)
