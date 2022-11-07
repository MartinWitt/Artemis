package de.tum.in.www1.artemis.versioning;

import static de.tum.in.www1.artemis.config.VersioningConfiguration.API_VERSIONS;

import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import de.tum.in.www1.artemis.exception.ApiVersionRangeNotValidException;

/**
 * A request condition for {@link VersionRanges} controlling a set of {@link VersionRange}s.
 */
public class VersionRangesRequestCondition implements RequestCondition<VersionRangesRequestCondition> {

    private final Set<VersionRange> ranges;

    // Comparison codes that specify whether two ranges collide or not
    private final Set<Integer> inRangeCodes = Set.of(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_INC_S, VersionRangeRequestCondition.VERSION_RANGE_CMP_S_INC_F,
            VersionRangeRequestCondition.VERSION_RANGE_CMP_F_EQS_S);

    public VersionRangesRequestCondition(VersionRange... ranges) {
        this(Arrays.asList(ranges));
    }

    /**
     * Creates a new instance with the given version ranges. Propagates null values.
     *
     * @param ranges the version ranges to use, nullable
     */
    public VersionRangesRequestCondition(Collection<VersionRange> ranges) {
        if (ranges != null) {
            var distinct = ranges.stream().distinct().toList();
            if (distinct.size() != 1 || distinct.get(0) != null) {
                this.ranges = Set.copyOf(ranges);
                return;
            }
        }
        this.ranges = null;
    }

    /**
     * Returns all valid version numbers from the given version ranges.
     * If ranges are null, all versions are valid.
     *
     * @return A list of valid version numbers
     */
    public List<Integer> getApplicableVersions() {
        if (ranges == null) {
            return API_VERSIONS;
        }
        return API_VERSIONS.stream().filter(e -> ranges.stream().anyMatch(range -> {
            try {
                return inRangeCodes.contains(VersionRangeRequestCondition.compare(range, VersionRangeRequestCondition.getInstanceOfVersionRange(e, e)));
            }
            catch (ApiVersionRangeNotValidException ex) {
                throw new RuntimeException(ex);
            }
        })).collect(Collectors.toList());
    }

    /**
     * Attempts to combine two {@link VersionRangesRequestCondition}s into one.
     *
     * @param other the condition to combine with.
     * @return the combined condition.
     * @throws ApiVersionRangeNotValidException If the value of a {@link VersionRange} is not valid
     */
    @Override
    public @NotNull VersionRangesRequestCondition combine(@NotNull VersionRangesRequestCondition other) {
        if (ranges == null || other.ranges == null) {
            return new VersionRangesRequestCondition((VersionRange) null);
        }

        // Separate ranges from start limits
        Set<VersionRangeRequestCondition> limits = new HashSet<>();
        Set<VersionRangeRequestCondition> ranges = new HashSet<>();
        Set<VersionRange> combinedRanges = new HashSet<>(other.ranges);
        combinedRanges.addAll(this.ranges);

        combinedRanges.forEach(range -> {
            if (range.value().length == 1) {
                limits.add(new VersionRangeRequestCondition(range.value()[0]));
            }
            else if (range.value().length == 2) {
                ranges.add(new VersionRangeRequestCondition(range.value()[0], range.value()[1]));
            }
            else {
                throw new ApiVersionRangeNotValidException();
            }
        });

        // Combine limits
        VersionRangeRequestCondition resultLimit = limits.stream().reduce(VersionRangeRequestCondition::combine).orElse(null);

        // Return new condition of limit if no ranges exist
        if (resultLimit != null && ranges.size() == 0) {
            return new VersionRangesRequestCondition(VersionRangeRequestCondition.getInstanceOfVersionRange(resultLimit.getVersions().get(0)));
        }

        // Combine limit with ranges
        return combineLimitAndRanges(resultLimit, ranges);
    }

    /**
     * Combines a limit with a list of ranges to a new condition.
     *
     * @param limit  The limit to combine with.
     * @param ranges The ranges to combine with.
     * @return The combined condition.
     */
    private VersionRangesRequestCondition combineLimitAndRanges(VersionRangeRequestCondition limit, Set<VersionRangeRequestCondition> ranges) {
        Integer limitStart = null;
        var rangePool = new ArrayList<>(ranges);
        List<VersionRangeRequestCondition> newRanges = new ArrayList<>();

        // If limit exists, ignore ranges within the limit, otherwise add all ranges to the pool
        if (limit != null) {
            limitStart = limit.getVersions().get(0);
            // Select ranges outside the limit
            while (!rangePool.isEmpty()) {
                var range = rangePool.remove(0);
                if (range.getVersions().get(0) < limitStart) {
                    if (range.getVersions().get(1) < limitStart) {
                        // range is completely before limit
                        newRanges.add(range);
                    }
                    else {
                        limitStart = range.getVersions().get(0);
                    }
                }
            }
        }
        else {
            newRanges.addAll(rangePool);
        }

        var simplifyRanges = simplifyRanges(newRanges);

        // Build new condition and return it
        List<VersionRange> annotationList = simplifyRanges.stream()
                .map(range -> VersionRangeRequestCondition.getInstanceOfVersionRange(range.getVersions().get(0), range.getVersions().get(1))).collect(Collectors.toList());
        if (limitStart != null) {
            annotationList.add(VersionRangeRequestCondition.getInstanceOfVersionRange(limitStart));
        }
        return new VersionRangesRequestCondition(annotationList);
    }

    /**
     * Simplifies a list of ranges by combining overlapping ranges.
     * @param rangePool The list of ranges to simplify.
     * @return The simplified list of ranges.
     */
    private List<VersionRangeRequestCondition> simplifyRanges(List<VersionRangeRequestCondition> rangePool) {
        List<VersionRangeRequestCondition> newRanges = new ArrayList<>();
        // As long as there are ranges in the pool, pop the first one and check against all other ranges
        // If there is an overlap, remove the second range, combine the two ranges, and add the new range to the pool
        while (!rangePool.isEmpty()) {
            var selectedCondition = rangePool.remove(0);
            boolean combined = false;
            for (VersionRangeRequestCondition condition : rangePool) {
                boolean canCombine = switch (selectedCondition.compareTo(condition)) {
                    case VersionRangeRequestCondition.VERSION_RANGE_CMP_F_CUT_S, VersionRangeRequestCondition.VERSION_RANGE_CMP_S_CUT_F -> false;
                    default -> true;
                };
                if (canCombine) {
                    rangePool.remove(condition);
                    rangePool.add(selectedCondition.combine(condition));
                    combined = true;
                    break;
                }
            }
            if (!combined) {
                newRanges.add(selectedCondition);
            }
        }
        return newRanges;
    }

    /**
     * Checks if the given condition collides with this condition.
     * @param versionRangesRequestCondition The condition to check.
     * @return True if the conditions collide, false otherwise.
     */
    public boolean collide(VersionRangesRequestCondition versionRangesRequestCondition) {
        Set<VersionRange> tbcRanges = versionRangesRequestCondition.getRanges();

        // If one of those represents all versions, the other one has to collide
        if (ranges == null || tbcRanges == null) {
            return true;
        }

        for (VersionRange range : ranges) {
            for (VersionRange tbcRange : tbcRanges) {
                int cmp = VersionRangeRequestCondition.compare(range, tbcRange);
                if (inRangeCodes.contains(cmp)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<VersionRange> getRanges() {
        return ranges;
    }

    @Override
    public int compareTo(@NotNull VersionRangesRequestCondition versionRangesRequestCondition, @NotNull HttpServletRequest httpServletRequest) {
        if (this.getMatchingCondition(httpServletRequest) == this) {
            return -1;
        }
        else if (versionRangesRequestCondition.getMatchingCondition(httpServletRequest) == versionRangesRequestCondition) {
            return 1;
        }
        else {
            return 0;
        }
    }

    /**
     * Returns the condition that matches the request or {@code null} if there is no match.
     *
     * @param request the current request
     * @return the matching condition or {@code null}
     */
    @Override
    public VersionRangesRequestCondition getMatchingCondition(@NotNull HttpServletRequest request) {
        try {
            String path = request.getRequestURI();
            if (!path.matches("^/?api/.*")) {
                return null;
            }
            // If any version is valid, return this
            if (ranges == null) {
                return this;
            }

            for (VersionRange range : ranges) {
                List<Integer> values = new ArrayList<>();
                for (int i = 0; i < range.value().length; i++) {
                    values.add(range.value()[i]);
                }
                if ((new VersionRangeRequestCondition(values)).getMatchingCondition(request) != null) {
                    return this;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VersionRangesRequestCondition that)) {
            return false;
        }
        if (ranges == null && that.ranges == null) {
            return true;
        }
        if (ranges == null || that.ranges == null) {
            return false;
        }

        var thisRanges = ranges.stream().map(range -> Arrays.stream(range.value()).boxed().toList()).collect(Collectors.toSet());
        var otherRanges = that.ranges.stream().map(range -> Arrays.stream(range.value()).boxed().toList()).collect(Collectors.toSet());

        return thisRanges.equals(otherRanges);
    }
}
