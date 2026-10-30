package com.acme.retail.ch18.support;

import com.acme.retail.ch18.order.Order;
import com.acme.retail.ch18.order.OrderRepository;
import com.acme.retail.ch18.security.InvalidToolInputException;
import com.acme.retail.ch18.security.OrderStatusView;
import com.acme.retail.ch18.security.PiiRedactionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * The only function-calling tool the AI assistant is allowed to invoke.
 *
 * <p>Function calling (also called "tool calling") is how an LLM (Large
 * Language Model) can trigger real code instead of only generating text
 * -- the model decides it needs an order status, and the framework runs
 * this Java method on its behalf. That power is exactly why this class
 * has to be narrowly scoped:
 *
 * <ul>
 *   <li><b>Least privilege</b> -- one method, one purpose (look up an
 *       order's status). No update, no delete, no "list all orders for a
 *       customer." The model cannot ask this tool for anything Sarah did
 *       not explicitly decide to expose.</li>
 *   <li><b>Input validation</b> -- {@link #lookupOrderStatus} rejects
 *       anything that is not shaped like a real order code before it
 *       touches the database. A model can be tricked by a user's message
 *       into calling this tool with strange input; the tool must not
 *       trust that input just because it came from the model.</li>
 *   <li><b>Output validation</b> -- the method returns an
 *       {@link OrderStatusView}, not the {@link Order} entity. Even if
 *       every other check failed, the payment detail and customer ID on
 *       {@code Order} physically cannot leave this method.</li>
 * </ul>
 */
@Component
public class OrderStatusLookupTool {

    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("^ORD-\\d{6}$");

    private final OrderRepository orderRepository;

    private final PiiRedactionService piiRedactionService;

    public OrderStatusLookupTool(OrderRepository orderRepository,
                                  PiiRedactionService piiRedactionService) {
        this.orderRepository = orderRepository;
        this.piiRedactionService = piiRedactionService;
    }

    @Tool(description = "Look up the delivery status of one Acme Retail order by its order code.")
    public OrderStatusView lookupOrderStatus(
            @ToolParam(description = "Order code, formatted like ORD-104822") String orderCode) {

        if (orderCode == null || !ORDER_CODE_PATTERN.matcher(orderCode.strip()).matches()) {
            // Deliberately vague: never echo the raw, possibly attacker-
            // controlled input back into an exception message or a log line.
            throw new InvalidToolInputException("Order code is not in a recognized format.");
        }

        Order order = orderRepository.findByOrderCode(orderCode.strip())
                .orElseThrow(() -> new InvalidToolInputException("No matching order was found."));

        return piiRedactionService.redactForPrompt(order);
    }
}
