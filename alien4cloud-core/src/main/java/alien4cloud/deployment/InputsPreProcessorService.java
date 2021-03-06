package alien4cloud.deployment;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import alien4cloud.common.MetaPropertiesService;
import alien4cloud.model.common.MetaPropConfiguration;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import alien4cloud.application.ApplicationService;
import alien4cloud.cloud.CloudService;
import alien4cloud.common.TagService;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.normative.ToscaFunctionConstants;

import com.google.common.collect.Maps;

/**
 * Inputs pre processor service manages pre-processing of inputs parameters in a Topology.
 *
 * Inputs in alien 4 cloud may be stored in the DeploymentSetup or may be injected from the cloud, application or environment (not implemented yet).
 */
@Slf4j
@Component
public class InputsPreProcessorService {
    private static final String CLOUD_META = "cloud_meta_";
    private static final String APP_META = "app_meta_";
    private static final String APP_TAGS = "app_tags_";

    @Resource
    private CloudService cloudService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private TagService tagService;
    @Resource
    private MetaPropertiesService metaPropertiesService;

    /**
     * Process the get inputs functions of a topology to inject actual input provided by the deployer user (from deployment setup) or from the cloud,
     * environment or application meta-properties.
     *
     * @param deploymentSetup The deployment setup that contains the input values.
     * @param topology The topology to process.
     * @param environment The environment instance linked to the deployment setup.
     */
    public void processGetInput(DeploymentSetup deploymentSetup, Topology topology, ApplicationEnvironment environment) {
        Map<String, String> inputs = getInputs(deploymentSetup, environment);
        if (topology.getNodeTemplates() != null) {
            for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
                processGetInput(inputs, nodeTemplate.getProperties());
                if (nodeTemplate.getRelationships() != null) {
                    for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                        processGetInput(inputs, relationshipTemplate.getProperties());
                    }
                }
                if (nodeTemplate.getCapabilities() != null) {
                    for (Capability capability : nodeTemplate.getCapabilities().values()) {
                        processGetInput(inputs, capability.getProperties());
                    }
                }
            }
        }
    }

    /**
     * Inputs can come from the deployer user (in such situations they are saved in the deployment setup) but also from the cloud or application
     * meta-properties.
     * This method creates a unified map of inputs to be injected in deployed applications.
     *
     * @param deploymentSetup The deployment setup.
     * @param environment The environment instance linked to the deployment setup.
     * @return A unified map of input for the topology containing the inputs from the deployment setup as well as the ones comming from cloud or application
     *         meta-properties.
     */
    private Map<String, String> getInputs(DeploymentSetup deploymentSetup, ApplicationEnvironment environment) {
        // initialize a map with input from the deployment setup
        Map<String, String> inputs = MapUtils.isEmpty(deploymentSetup.getInputProperties()) ? Maps.<String, String> newHashMap() : deploymentSetup
                .getInputProperties();

        // Map id -> value of meta properties from cloud or application.
        Map<String, String> metaPropertiesValuesMap = Maps.newHashMap();

        // add the inputs from the cloud meta-properties
        if (environment.getCloudId() != null) {
            Cloud cloud = cloudService.get(environment.getCloudId());
            if (cloud.getMetaProperties() != null) {
                metaPropertiesValuesMap.putAll(cloud.getMetaProperties());
            }
        }
        // and the ones from the application meta-properties
        // meta or tags from application
        if (environment.getApplicationId() != null) {
            Application application = applicationService.getOrFail(environment.getApplicationId());
            if (application.getMetaProperties() != null) {
                metaPropertiesValuesMap.putAll(application.getMetaProperties());
            }

            Map<String, String> tags = tagService.tagListToMap(application.getTags());
            prefixAndAddContextInput(inputs, InputsPreProcessorService.CLOUD_META, tags, false);
        }

        // inputs from the cloud starts with
        prefixAndAddContextInput(inputs, InputsPreProcessorService.CLOUD_META, metaPropertiesValuesMap, true);

        return inputs;
    }

    /**
     * Add the context inputs to the actual deployment inputs by adding the given prefix to the key.
     *
     * @param inputs The inputs to be used for the topology deployment.
     * @param prefix The prefix to be added to the context inputs.
     * @param contextInputs The map of inputs from context elements (cloud, application, environment).
     */
    private void prefixAndAddContextInput(Map<String, String> inputs, String prefix, Map<String, String> contextInputs, boolean isMeta) {
        if (contextInputs == null || contextInputs.isEmpty()) {
            return; // no inputs to add.
        }
        if (isMeta) {
            String[] ids = new String[contextInputs.size()];
            int i = 0;
            for (String id : contextInputs.keySet()) {
                ids[i] = id;
                i++;
            }
            Map<String, MetaPropConfiguration> configurationMap = metaPropertiesService.getByIds(ids);
            for (Map.Entry<String, String> contextInputEntry : contextInputs.entrySet()) {
                inputs.put(prefix + configurationMap.get(contextInputEntry.getKey()).getName(), contextInputEntry.getValue());
            }
        } else {
            for (Map.Entry<String, String> contextInputEntry : contextInputs.entrySet()) {
                inputs.put(prefix + contextInputEntry.getKey(), contextInputEntry.getValue());
            }
        }
    }

    private void processGetInput(Map<String, String> inputs, Map<String, AbstractPropertyValue> properties) {
        if (properties != null) {
            for (Map.Entry<String, AbstractPropertyValue> propEntry : properties.entrySet()) {
                if (propEntry.getValue() instanceof FunctionPropertyValue) {
                    FunctionPropertyValue function = (FunctionPropertyValue) propEntry.getValue();
                    if (ToscaFunctionConstants.GET_INPUT.equals(function.getFunction())) {
                        String inputName = function.getParameters().get(0);
                        // replace the value from the inputs
                        ScalarPropertyValue value = new ScalarPropertyValue(inputs.get(inputName));
                        propEntry.setValue(value);
                    } else {
                        log.warn("Function detected for property <{}> while only get_input should be authorized.", propEntry.getKey());
                    }
                }
            }
        }
    }
}
