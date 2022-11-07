package de.tum.in.www1.artemis.versioning;

import static de.tum.in.www1.artemis.versioning.VersionRangeRequestCondition.getInstanceOfVersionRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.exception.ApiVersionRangeNotValidException;

public class VersionRangeRequestConditionTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    private final VersionRange range1_5 = getInstanceOfVersionRange(1, 5);

    private final VersionRange range2_5 = getInstanceOfVersionRange(2, 5);

    private final VersionRange range1_6 = getInstanceOfVersionRange(1, 6);

    private final VersionRange range2_6 = getInstanceOfVersionRange(2, 6);

    private final VersionRange range5_9 = getInstanceOfVersionRange(5, 9);

    private final VersionRange range6_10 = getInstanceOfVersionRange(6, 10);

    private final VersionRange range7_11 = getInstanceOfVersionRange(7, 11);

    private final VersionRange limit1 = getInstanceOfVersionRange(1);

    private final VersionRange limit5 = getInstanceOfVersionRange(5);

    private final VersionRange limit6 = getInstanceOfVersionRange(6);

    private final VersionRange limit7 = getInstanceOfVersionRange(7);

    private final VersionRangeRequestCondition conditionRange1_5 = createCondition(1, 5);

    private final VersionRangeRequestCondition conditionRange3_5 = createCondition(3, 5);

    private final VersionRangeRequestCondition conditionRange3_9 = createCondition(3, 9);

    private final VersionRangeRequestCondition conditionRange4_9 = createCondition(4, 9);

    private final VersionRangeRequestCondition conditionRange5_9 = createCondition(5, 9);

    private final VersionRangeRequestCondition conditionRange6_9 = createCondition(6, 9);

    private final VersionRangeRequestCondition conditionRange7_9 = createCondition(7, 9);

    private final VersionRangeRequestCondition conditionLimit1 = createCondition(1);

    private final VersionRangeRequestCondition conditionLimit2 = createCondition(2);

    private final VersionRangeRequestCondition conditionLimit3 = createCondition(3);

    private final VersionRangeRequestCondition conditionLimit5 = createCondition(5);

    private final VersionRangeRequestCondition conditionLimit6 = createCondition(6);

    private final VersionRangeRequestCondition conditionLimit7 = createCondition(7);

    @Test
    void testCompare_rangesEqual() {
        assertThat(VersionRangeRequestCondition.compare(range1_5, range1_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_EQS_S);
        assertThat(VersionRangeRequestCondition.compare(limit1, limit1)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_EQS_S);
    }

    @Test
    void testCompare_firstRangeIncludesSecond() {
        assertThat(VersionRangeRequestCondition.compare(range1_6, range1_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_INC_S);
        assertThat(VersionRangeRequestCondition.compare(range1_5, range2_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_INC_S);
        assertThat(VersionRangeRequestCondition.compare(limit1, range1_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_INC_S);
        assertThat(VersionRangeRequestCondition.compare(limit1, range2_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_INC_S);
        assertThat(VersionRangeRequestCondition.compare(limit1, limit7)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_INC_S);
    }

    @Test
    void testCompare_secondRangeIncludesFirst() {
        assertThat(VersionRangeRequestCondition.compare(range1_5, range1_6)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_INC_F);
        assertThat(VersionRangeRequestCondition.compare(range2_5, range1_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_INC_F);
        assertThat(VersionRangeRequestCondition.compare(range1_5, limit1)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_INC_F);
        assertThat(VersionRangeRequestCondition.compare(range2_5, limit1)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_INC_F);
        assertThat(VersionRangeRequestCondition.compare(limit7, limit1)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_INC_F);
    }

    @Test
    void testCompare_noIntersectSecondRangeStartsFirst() {
        assertThat(VersionRangeRequestCondition.compare(range7_11, range1_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_CUT_F);
        assertThat(VersionRangeRequestCondition.compare(limit7, range1_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_CUT_F);
    }

    @Test
    void testCompare_shiftSecondRangeStartsFirst() {
        assertThat(VersionRangeRequestCondition.compare(range2_6, range1_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_THEN_F);
        assertThat(VersionRangeRequestCondition.compare(range5_9, range1_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_THEN_F);
        assertThat(VersionRangeRequestCondition.compare(range6_10, range1_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_THEN_F);
        assertThat(VersionRangeRequestCondition.compare(limit5, range2_6)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_THEN_F);
        assertThat(VersionRangeRequestCondition.compare(limit6, range2_6)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_THEN_F);
        assertThat(VersionRangeRequestCondition.compare(limit6, range1_5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_S_THEN_F);
    }

    @Test
    void testCompare_shiftFirstRangeStartsFirst() {
        assertThat(VersionRangeRequestCondition.compare(range1_5, range2_6)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_THEN_S);
        assertThat(VersionRangeRequestCondition.compare(range1_5, range5_9)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_THEN_S);
        assertThat(VersionRangeRequestCondition.compare(range1_5, range6_10)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_THEN_S);
        assertThat(VersionRangeRequestCondition.compare(range2_6, limit5)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_THEN_S);
        assertThat(VersionRangeRequestCondition.compare(range2_6, limit6)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_THEN_S);
        assertThat(VersionRangeRequestCondition.compare(range1_5, limit6)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_THEN_S);
    }

    @Test
    void testCompare_noIntersectFirstRangeStartsFirst() {
        assertThat(VersionRangeRequestCondition.compare(range1_5, range7_11)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_CUT_S);
        assertThat(VersionRangeRequestCondition.compare(range1_5, limit7)).isEqualTo(VersionRangeRequestCondition.VERSION_RANGE_CMP_F_CUT_S);
    }

    @Test
    void testCombine_separateRanges() {
        assertThrows(ApiVersionRangeNotValidException.class, () -> conditionRange7_9.combine(conditionRange3_5));
        assertThrows(ApiVersionRangeNotValidException.class, () -> conditionRange3_5.combine(conditionRange7_9));
    }

    @Test
    void testCombine_equalRanges() {
        List<Integer> actualRangesSameVersions = conditionRange3_5.combine(conditionRange3_5).getVersions();
        assertThat(actualRangesSameVersions).isEqualTo(List.of(3, 5));
    }

    @Test
    void testCombine_neighboringRanges() {
        var actual1 = conditionRange6_9.combine(conditionRange3_5).getVersions();
        var actual2 = conditionRange3_5.combine(conditionRange6_9).getVersions();
        assertThat(actual1).isEqualTo(List.of(3, 9));
        assertThat(actual2).isEqualTo(List.of(3, 9));
    }

    @Test
    void testCombine_neighboringRanges_startEndSame() {
        var actual1 = conditionRange5_9.combine(conditionRange3_5).getVersions();
        var actual2 = conditionRange3_5.combine(conditionRange5_9).getVersions();
        assertThat(actual1).isEqualTo(List.of(3, 9));
        assertThat(actual2).isEqualTo(List.of(3, 9));
    }

    @Test
    void testCombine_overlappingRanges() {
        var actual1 = conditionRange4_9.combine(conditionRange3_5).getVersions();
        var actual2 = conditionRange3_5.combine(conditionRange4_9).getVersions();
        assertThat(actual1).isEqualTo(List.of(3, 9));
        assertThat(actual2).isEqualTo(List.of(3, 9));
    }

    @Test
    void testCombine_ranges_sameStart_differentEnd() {
        var actual1 = conditionRange3_9.combine(conditionRange3_5).getVersions();
        var actual2 = conditionRange3_5.combine(conditionRange3_9).getVersions();
        assertThat(actual1).isEqualTo(List.of(3, 9));
        assertThat(actual2).isEqualTo(List.of(3, 9));
    }

    @Test
    void testCombine_ranges_differentStart_sameEnd() {
        var actual1 = conditionRange1_5.combine(conditionRange3_5).getVersions();
        var actual2 = conditionRange3_5.combine(conditionRange1_5).getVersions();
        assertThat(actual1).isEqualTo(List.of(1, 5));
        assertThat(actual2).isEqualTo(List.of(1, 5));
    }

    @Test
    void testCombine_limitNeighboringRangeStart() {
        var actual1 = conditionLimit2.combine(conditionRange3_5).getVersions();
        var actual2 = conditionRange3_5.combine(conditionLimit2).getVersions();
        assertThat(actual1).isEqualTo(List.of(2));
        assertThat(actual2).isEqualTo(List.of(2));
    }

    @Test
    void testCombine_limitOnStartOfRange() {
        var actual1 = conditionLimit3.combine(conditionRange3_5).getVersions();
        var actual2 = conditionRange3_5.combine(conditionLimit3).getVersions();
        assertThat(actual1).isEqualTo(List.of(3));
        assertThat(actual2).isEqualTo(List.of(3));
    }

    @Test
    void testCombine_limitOnEndOfRange() {
        var actual1 = conditionLimit5.combine(conditionRange3_5).getVersions();
        var actual2 = conditionRange3_5.combine(conditionLimit5).getVersions();
        assertThat(actual1).isEqualTo(List.of(3));
        assertThat(actual2).isEqualTo(List.of(3));
    }

    @Test
    void testCombine_limitNeighboringRangeEnd() {
        var actual1 = conditionLimit6.combine(conditionRange3_5).getVersions();
        var actual2 = conditionRange3_5.combine(conditionLimit6).getVersions();
        assertThat(actual1).isEqualTo(List.of(3));
        assertThat(actual2).isEqualTo(List.of(3));
    }

    @Test
    void testCombine_limitBeforeRange() {
        var actual1 = conditionLimit1.combine(conditionRange3_5).getVersions();
        var actual2 = conditionRange3_5.combine(conditionLimit1).getVersions();
        assertThat(actual1).isEqualTo(List.of(1));
        assertThat(actual2).isEqualTo(List.of(1));
    }

    @Test
    void testCombine_limitAfterRange() {
        assertThrows(ApiVersionRangeNotValidException.class, () -> conditionLimit7.combine(conditionRange3_5));
        assertThrows(ApiVersionRangeNotValidException.class, () -> conditionRange3_5.combine(conditionLimit7));
    }

    @Test
    void testCombine_sameLimits() {
        List<Integer> actual = conditionLimit1.combine(conditionLimit1).getVersions();
        assertEquals(List.of(1), actual);
        assertThat(actual).isEqualTo(List.of(1));

    }

    @Test
    void testCombine_differentLimits() {
        List<Integer> actual1 = conditionLimit1.combine(conditionLimit2).getVersions();
        List<Integer> actual2 = conditionLimit2.combine(conditionLimit1).getVersions();
        assertEquals(List.of(1), actual1);
        assertEquals(List.of(1), actual2);
        assertThat(actual1).isEqualTo(List.of(1));
        assertThat(actual2).isEqualTo(List.of(1));
    }

    @Test
    void testGetMatchingConditionWrongBase() {
        VersionRangeRequestCondition condition = new VersionRangeRequestCondition(1);
        DummyRequest dummyRequest = new DummyRequest();

        dummyRequest.setRequestURI("/something/foo");
        assertThat(condition.getMatchingCondition(dummyRequest)).isNull();
    }

    @Test
    void testGetMatchingConditionNoVersion() {
        VersionRangeRequestCondition condition = new VersionRangeRequestCondition(1);
        DummyRequest dummyRequest = new DummyRequest();

        dummyRequest.setRequestURI("/api/something/foo");
        assertThat(condition.getMatchingCondition(dummyRequest)).isSameAs(condition);
    }

    @Test
    void testGetMatchingConditionInvalidVersion() {
        VersionRangeRequestCondition condition = new VersionRangeRequestCondition(1);
        DummyRequest dummyRequest = new DummyRequest();
        dummyRequest.setRequestURI("/api/v13/something/foo");
        assertThat(condition.getMatchingCondition(dummyRequest)).isNull();
    }

    @Test
    void testGetMatchingConditionRootURI() {
        VersionRangeRequestCondition condition = new VersionRangeRequestCondition(1);
        DummyRequest dummyRequest = new DummyRequest();
        dummyRequest.setRequestURI("/");
        assertThat(condition.getMatchingCondition(dummyRequest)).isNull();
    }

    @Test
    void testGetMatchingConditionLimit() {
        VersionRangeRequestCondition condition = new VersionRangeRequestCondition(2);
        DummyRequest dummyRequest = new DummyRequest();

        // Before Limit
        dummyRequest.setRequestURI("/api/v1/something/foo");
        assertThat(condition.getMatchingCondition(dummyRequest)).isNull();

        // At start of limit
        dummyRequest.setRequestURI("/api/v2/something/foo");
        assertThat(condition.getMatchingCondition(dummyRequest)).isSameAs(condition);

        // Within limit
        dummyRequest.setRequestURI("/api/v3/something/foo");
        assertThat(condition.getMatchingCondition(dummyRequest)).isSameAs(condition);
    }

    @Test
    void testGetMatchingConditionRange() {
        VersionRangeRequestCondition condition = new VersionRangeRequestCondition(2, 4);
        DummyRequest dummyRequest = new DummyRequest();
        // Before range
        dummyRequest.setRequestURI("/api/v1/something/foo");
        assertThat(condition.getMatchingCondition(dummyRequest)).isNull();
        // At start of range
        dummyRequest.setRequestURI("/api/v2/something/foo");
        assertThat(condition.getMatchingCondition(dummyRequest)).isSameAs(condition);
        // within range
        dummyRequest.setRequestURI("/api/v3/something/foo");
        assertThat(condition.getMatchingCondition(dummyRequest)).isSameAs(condition);
        // At end of range
        dummyRequest.setRequestURI("/api/v4/something/foo");
        assertThat(condition.getMatchingCondition(dummyRequest)).isSameAs(condition);
        // After range
        dummyRequest.setRequestURI("/api/v5/something/foo");
        assertThat(condition.getMatchingCondition(dummyRequest)).isNull();
    }

    private VersionRangeRequestCondition createCondition(int... values) {
        List<Integer> list = new ArrayList<>();
        for (int value : values) {
            list.add(value);
        }
        return new VersionRangeRequestCondition(list);
    }

    private VersionRangeRequestCondition createCondition(int value) {
        return createCondition(new int[] { value });
    }
}
