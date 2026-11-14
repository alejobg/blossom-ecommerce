package com.blossom.ecommerce.order.application;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderItem;
import com.blossom.ecommerce.order.domain.port.in.OrderLine;
import com.blossom.ecommerce.order.domain.port.in.OrderUseCase;
import com.blossom.ecommerce.order.domain.port.in.PlaceOrderCommand;
import com.blossom.ecommerce.order.domain.port.out.OrderRepositoryPort;
import com.blossom.ecommerce.product.domain.port.in.ProductUseCase;
import com.blossom.ecommerce.product.domain.port.in.ReservedStock;
import com.blossom.ecommerce.product.domain.port.in.StockRequest;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Talks to the product module only through {@link ProductUseCase}, never through its repository.
 */
@Service
@Transactional
public class OrderService implements OrderUseCase {

    private static final String RESOURCE = "Order";

    private final OrderRepositoryPort orderRepository;
    private final ProductUseCase productUseCase;

    public OrderService(OrderRepositoryPort orderRepository, ProductUseCase productUseCase) {
        this.orderRepository = orderRepository;
        this.productUseCase = productUseCase;
    }

    /**
     * One transaction: the stock reservation joins it, so if any line fails (or saving the order fails)
     * no stock is taken and no order is stored.
     */
    @Override
    public Order place(PlaceOrderCommand command) {
        Order.requireDistinctProducts(command.lines().stream().map(OrderLine::productId).toList());

        List<StockRequest> requests = command.lines().stream()
                .map(line -> new StockRequest(line.productId(), line.quantity()))
                .toList();
        List<OrderItem> items = productUseCase.reserveStock(requests).stream()
                .map(OrderService::toItem)
                .toList();

        return orderRepository.save(Order.place(command.customerId(), items));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Order> listForCustomer(Long customerId, PageQuery pageQuery) {
        return orderRepository.findByCustomerId(customerId, pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getForCustomer(Long customerId, Long orderId) {
        // Someone else's order is reported as missing, so ids of other customers' orders are not revealed
        return orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, orderId));
    }

    private static OrderItem toItem(ReservedStock reserved) {
        return OrderItem.of(reserved.productId(), reserved.productName(), reserved.unitPrice(), reserved.quantity());
    }
}
