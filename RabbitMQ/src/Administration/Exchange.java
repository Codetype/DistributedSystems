package Administration;

import com.rabbitmq.client.BuiltinExchangeType;

public interface Exchange {
    String Name = "EXCHANGE";
    BuiltinExchangeType Type = BuiltinExchangeType.TOPIC;
}
