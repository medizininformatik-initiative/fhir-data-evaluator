package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.fhirpath.IFhirPathEvaluationContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds a resource and additional 'include' resources from the bundle.
 * <p>
 * The includes are all resources from the bundle with the search mode 'include'. They are used to resolve references
 * in the stratifier.
 *
 * @param mainResource      the main resource that will be evaluated
 * @param includes          maps the reference uri string to the corresponding referenced resource
 * @param fhirPathEngine    the fhirPathEngine that is used to evaluate the resource later
 */
public record ResourceWithIncludes(Resource mainResource, Map<String, Resource> includes, IFhirPath fhirPathEngine) {


    /**
     * Sets an evaluation context that uses the 'include' resources to resolve references.
     */
    public static void setResolver(IFhirPath fhirPathEngine, Map<String, Resource> includes) {
        IFhirPathEvaluationContext evaluationContext = new IFhirPathEvaluationContext() {
            @Override
            public IBase resolveReference(@Nonnull IIdType theReference, @Nullable IBase theContext) {
                return includes.get(theReference.getValue());
            }
        };
        fhirPathEngine.setEvaluationContext(evaluationContext);
    }

    /**
     * Creates a {@link ResourceWithIncludes} for every resource with search type 'match' and appends the resources with
     * search typ 'include' to the {@link ResourceWithIncludes} if there are any.
     * <p>
     * Also, if there are 'include' resources, a resolver has to be set that uses the 'include' resources to resolve
     * references in the FHIRPath of the stratifiers.
     *
     * @param bundle                    the raw bundle with 'match' and 'include' resources
     * @param applicationFhirPathEngine the fhir path engine of the spring application that is used to evaluate resources
     *                                  without 'include' resources
     * @param context                   the {@link FhirContext} to create a new fhir path engine that is used for all
     *                                  resources in this bundle in case there are 'include' resources
     * @return a stream of all resulting {@link ResourceWithIncludes}
     */
    public static Stream<ResourceWithIncludes> processBundleIncludes(Bundle bundle, IFhirPath applicationFhirPathEngine, FhirContext context) {
        Map<String, Resource> includes = bundle.getEntry().stream()
                .filter(e -> e.getSearch().getMode().equals(Bundle.SearchEntryMode.INCLUDE))
                .collect(Collectors.toMap(e -> e.getResource().fhirType() + "/" + e.getResource().getIdPart(), Bundle.BundleEntryComponent::getResource));

        IFhirPath fhirPathEngine;
        if (includes.isEmpty()) {
            fhirPathEngine = applicationFhirPathEngine;
        } else {
            fhirPathEngine = context.newFhirPath();
            setResolver(fhirPathEngine, includes);
        }

        return bundle.getEntry().stream().filter(e -> e.getSearch().getMode().equals(Bundle.SearchEntryMode.MATCH))
                .map(match -> new ResourceWithIncludes(match.getResource(), includes, fhirPathEngine));
    }
}
