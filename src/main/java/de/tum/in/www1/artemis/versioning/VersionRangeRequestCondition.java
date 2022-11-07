package de.tum.in.www1.artemis.versioning;

import static de.tum.in.www1.artemis.config.VersioningConfiguration.API_VERSIONS;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import de.tum.in.www1.artemis.exception.ApiVersionRangeNotValidException;

/**
 * A request condition for {@link VersionRange} controlling one {@link VersionRange}.
 */
public class VersionRangeRequestCondition implements RequestCondition<VersionRangeRequestCondition> {

    // Ranges don't intersect, second range starts before first
    public static final int VERSION_RANGE_CMP_S_CUT_F = -3;

    // Second range starts before first range starts, second range ends before first range ends ("shift")
    public static final int VERSION_RANGE_CMP_S_THEN_F = -2;

    // Second range includes first range
    public static final int VERSION_RANGE_CMP_S_INC_F = -1;

    // First element equals second
    public static final int VERSION_RANGE_CMP_F_EQS_S = 0;

    // First range includes second range
    public static final int VERSION_RANGE_CMP_F_INC_S = 1;

    // First range starts before second range starts, first range ends before second range ends ("shift")
    public static final int VERSION_RANGE_CMP_F_THEN_S = 2;

    // Ranges don't intersect, first range starts before second
    public static final int VERSION_RANGE_CMP_F_CUT_S = 3;

    private final List<Integer> versions;

    public VersionRangeRequestCondition(VersionRange versionRange) {
        this.versions = new ArrayList<>();
        int[] versions = versionRange.value();
        for (int version : versions) {
            this.versions.add(version);
        }
    }

    public VersionRangeRequestCondition(Integer... versions) {
        this(Arrays.asList(versions));
    }

    public VersionRangeRequestCondition(Collection<Integer> versions) {
        this.versions = List.copyOf(versions);
    }

    public List<Integer> getVersions() {
        return versions;
    }

    /**
     * Attempts to combine two {@link VersionRangeRequestCondition}s into one.
     *
     * @param other the condition to combine with.
     * @return the combined condition.
     * @throws ApiVersionRangeNotValidException If the value of a {@link VersionRange} is not valid
     */
    @Override
    public @NotNull VersionRangeRequestCondition combine(@NotNull VersionRangeRequestCondition other) {
        List<Integer> versions1 = versions;
        List<Integer> versions2 = other.versions;
        if (versions1.size() == 2 && versions2.size() == 2) {
            // Both are finite. If they don't overlap, we can't combine them, otherwise we return the combined range
            if ((versions1.get(1) < versions2.get(0) && versions1.get(1) + 1 < versions2.get(0))
                    || (versions2.get(1) < versions1.get(0) && versions2.get(1) + 1 < versions1.get(0))) {
                // Not concatenated ranges => not valid combination
                throw new ApiVersionRangeNotValidException();
            }
            else {
                int newStart = versions1.get(0) < versions2.get(0) ? versions1.get(0) : versions2.get(0);
                int newEnd = versions1.get(1) > versions2.get(1) ? versions1.get(1) : versions2.get(1);
                return new VersionRangeRequestCondition(newStart, newEnd);
            }
        }
        else if ((versions1.size() == 2 && versions2.size() == 1) || (versions1.size() == 1 && versions2.size() == 2)) {
            // One is finite and one is infinite.
            int limit = versions1.size() == 1 ? versions1.get(0) : versions2.get(0);
            List<Integer> range = versions1.size() == 2 ? versions1 : versions2;

            if (range.get(1) + 1 < limit) {
                // there is a range and then there is a start limit afterwards => not a valid combination
                throw new ApiVersionRangeNotValidException();
            }
            else if (limit <= range.get(0)) {
                // start limit dominates range (includes)
                return new VersionRangeRequestCondition(limit);
            }
            else {
                // start limit is within the range => create a start limit from the beginning of range
                return new VersionRangeRequestCondition(range.get(0));
            }
        }
        else if (versions1.size() == 1 && versions2.size() == 1) {
            // Both are infinite. Redefine start limit
            return new VersionRangeRequestCondition(versions1.get(0) < versions2.get(0) ? versions1.get(0) : versions2.get(0));
        }

        // There should be no other case as we except both lists to be of size 1 or 2
        throw new ApiVersionRangeNotValidException();
    }

    /**
     * Returns the condition that matches the request or {@code null} if there is no match.
     *
     * @param request the current request
     * @return the matching condition or {@code null}
     */
    @Override
    public VersionRangeRequestCondition getMatchingCondition(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.matches("^/?api/.*")) {
            return null;
        }
        String urlString = path.startsWith("/") ? path.substring(1) : path;
        String[] parts = urlString.split("/");
        if (parts.length < 2) {
            // No path specified "/api"
            return null;
        }

        if (parts[1].matches("^v\\d*$")) {
            int requestedVersion = Integer.parseInt(parts[1].substring(1));
            return checkVersion(requestedVersion) ? this : null;
        }
        else {
            // no version found, assume the latest version
            int latestVersion = API_VERSIONS.get(API_VERSIONS.size() - 1);
            return checkVersion(latestVersion) ? this : null;
        }

    }

    /**
     * Factory method for {@link VersionRange} instances.
     *
     * @param value the version numbers to pass
     * @return the created {@link VersionRange} instance
     */
    public static VersionRange getInstanceOfVersionRange(int... value) {
        return new VersionRange() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return VersionRange.class;
            }

            @Override
            public int[] value() {
                return value;
            }
        };
    }

    /**
     * Compares the provided condition to this condition
     *
     * @param other The other condition
     * @return Results see VERSION_RANGE_CMP_* in {@link VersionRangeRequestCondition}
     */
    public int compareTo(VersionRangeRequestCondition other) {
        var v1 = this.versions;
        var v2 = other.versions;
        if (v1.size() == 1 && v2.size() == 1) {
            return compareTwoLimits(v1, v2);
        }
        else if (v1.size() == 2 && v2.size() == 2) {
            return compareTwoRanges(v1, v2);
        }
        else if (v1.size() == 1 && v2.size() == 2) {
            return compareLimitAndRange(v1.get(0), v2);
        }
        else if (v1.size() == 2 && v2.size() == 1) {
            return -compareLimitAndRange(v2.get(0), v1);
        }
        throw new ApiVersionRangeNotValidException();
    }

    /**
     * @param v1 Item containing two versions sorted ASC
     * @param v2 Item to be compared to containing two versions sorted ASC
     * @return Results see VERSION_RANGE_CMP_* in {@link VersionRangeRequestCondition}
     */
    public static int compare(VersionRange v1, VersionRange v2) {
        return new VersionRangeRequestCondition(v1).compareTo(new VersionRangeRequestCondition(v2));
    }

    private int compareTwoLimits(List<Integer> limit1, List<Integer> limit2) {
        var result = Integer.compare(limit1.get(0), limit2.get(0));
        // This could be simplified but for better readability (Constants) it is not
        if (result < 0) {
            // First limit starts after second one. Since both are infinite second one includes first one
            return VERSION_RANGE_CMP_F_INC_S;
        }
        else if (result > 0) {
            // First limit starts before second one. Since both are infinite first one includes second one
            return VERSION_RANGE_CMP_S_INC_F;
        }
        else {
            // Both start the same point
            return VERSION_RANGE_CMP_F_EQS_S;
        }
    }

    private int compareLimitAndRange(int limitVersion, List<Integer> range) {
        var result = Integer.compare(limitVersion, range.get(0));
        if (result < 1) {
            // Range starts with or after the limit starts
            return VERSION_RANGE_CMP_F_INC_S;
        }
        else if (range.get(1) + 1 < limitVersion) {
            // Range starts end ends before the limit
            return VERSION_RANGE_CMP_S_CUT_F;
        }
        else {
            // Range starts before the limit does, but intersects
            return VERSION_RANGE_CMP_S_THEN_F;
        }
    }

    private int compareTwoRanges(List<Integer> range1, List<Integer> range2) {
        int startResult = Integer.compare(range1.get(0), range2.get(0));
        int endResult = Integer.compare(range1.get(1), range2.get(1));

        if (startResult == 0 && endResult == 0) {
            return VERSION_RANGE_CMP_F_EQS_S;
        }
        else if (startResult == 0) {
            if (endResult < 0) {
                return VERSION_RANGE_CMP_S_INC_F;
            }
            else {
                return VERSION_RANGE_CMP_F_INC_S;
            }
        }
        else if (endResult == 0) {
            if (startResult < 0) {
                return VERSION_RANGE_CMP_F_INC_S;
            }
            else {
                return VERSION_RANGE_CMP_S_INC_F;
            }
        }
        else if (startResult < 0) {
            if (endResult > -1) {
                return VERSION_RANGE_CMP_F_INC_S;
            }
            if (range1.get(1) + 1 < range2.get(0)) {
                return VERSION_RANGE_CMP_F_CUT_S;
            }
            else {
                return VERSION_RANGE_CMP_F_THEN_S;
            }
        }
        else {
            if (endResult < 1) {
                return VERSION_RANGE_CMP_S_INC_F;
            }
            if (range2.get(1) + 1 < range1.get(0)) {
                return VERSION_RANGE_CMP_S_CUT_F;
            }
            else {
                return VERSION_RANGE_CMP_S_THEN_F;
            }
        }
    }

    @Override
    public int compareTo(@NotNull VersionRangeRequestCondition versionRangeRequestCondition, @NotNull HttpServletRequest httpServletRequest) {
        System.err.println("VersionRangeRequestCondition can't be compared. It either fits or it doesn't.");
        return 0;
    }

    /**
     * Check if the requested version is part of the version range
     *
     * @param requestedVersion the requested version
     * @return true if the requested version is part of the version range
     */
    private boolean checkVersion(int requestedVersion) {
        // only allowed versions here
        if (API_VERSIONS.contains(requestedVersion)) {
            int startVersion = this.versions.get(0);

            return requestedVersion >= startVersion && (versions.size() == 1 || requestedVersion <= versions.get(1));
        }
        return false;
    }
}
