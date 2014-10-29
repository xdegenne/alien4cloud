package alien4cloud.tosca.parser.mapping;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.model.PropertyDefinition;
import alien4cloud.tosca.parser.TypeNodeParser;

@Component
public class Wd03AttributeDefinition extends AbstractMapper<PropertyDefinition> {

    public Wd03AttributeDefinition() {
        super(new TypeNodeParser<PropertyDefinition>(PropertyDefinition.class, "Attribute definition"));
    }

    @Override
    public void initMapping() {
        quickMap("type");
        quickMap("description");
        quickMap("default");
    }
}