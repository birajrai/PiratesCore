package ovh.paulem.btm.utils;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.entity.Player;

public class MathUtils {
    public static int constrainToRange(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static double evaluate(String js, Player player) {
        Expression expression = new ExpressionBuilder(js)
                .variables("x")
                .build()
                .setVariable("x", ExperienceUtils.getPlayerXP(player));
        return expression.evaluate();
    }
}
