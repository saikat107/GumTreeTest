package edu.virginia.cs.gumtreetest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OneLineBugs {
	public static String[] ALL_ONE_LINE_BUGS = {"/zf8/sc2nf/CCRecom_exp/Defects4j/Chart/1/parent/source_org_jfree_chart_renderer_category_AbstractCategoryItemRenderer.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Chart/8/parent/source_org_jfree_data_time_Week.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Chart/10/parent/source_org_jfree_chart_imagemap_StandardToolTipTagFragmentGenerator.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Chart/11/parent/source_org_jfree_chart_util_ShapeUtilities.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Chart/12/parent/source_org_jfree_chart_plot_MultiplePiePlot.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Chart/20/parent/source_org_jfree_chart_plot_ValueMarker.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Chart/24/parent/source_org_jfree_chart_renderer_GrayPaintScale.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Closure/10/parent/src_com_google_javascript_jscomp_NodeUtil.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Closure/14/parent/src_com_google_javascript_jscomp_ControlFlowAnalysis.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Closure/38/parent/src_com_google_javascript_jscomp_CodeConsumer.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Closure/70/parent/src_com_google_javascript_jscomp_TypedScopeCreator.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Closure/73/parent/src_com_google_javascript_jscomp_CodeGenerator.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Closure/86/parent/src_com_google_javascript_jscomp_NodeUtil.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Closure/92/parent/src_com_google_javascript_jscomp_ProcessClosurePrimitives.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Closure/93/parent/src_com_google_javascript_jscomp_ProcessClosurePrimitives.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Closure/123/parent/src_com_google_javascript_jscomp_CodeGenerator.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Closure/130/parent/src_com_google_javascript_jscomp_CollapseProperties.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Lang/6/parent/src_main_java_org_apache_commons_lang3_text_translate_CharSequenceTranslator.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Lang/21/parent/src_main_java_org_apache_commons_lang3_time_DateUtils.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Lang/24/parent/src_main_java_org_apache_commons_lang3_math_NumberUtils.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Lang/26/parent/src_main_java_org_apache_commons_lang3_time_FastDateFormat.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Lang/57/parent/src_java_org_apache_commons_lang_LocaleUtils.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Lang/59/parent/src_java_org_apache_commons_lang_text_StrBuilder.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Lang/61/parent/src_java_org_apache_commons_lang_text_StrBuilder.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/5/parent/src_main_java_org_apache_commons_math3_complex_Complex.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/11/parent/src_main_java_org_apache_commons_math3_distribution_MultivariateNormalDistribution.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/30/parent/src_main_java_org_apache_commons_math3_stat_inference_MannWhitneyUTest.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/33/parent/src_main_java_org_apache_commons_math3_optimization_linear_SimplexTableau.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/34/parent/src_main_java_org_apache_commons_math3_genetics_ListPopulation.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/57/parent/src_main_java_org_apache_commons_math_stat_clustering_KMeansPlusPlusClusterer.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/58/parent/src_main_java_org_apache_commons_math_optimization_fitting_GaussianFitter.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/59/parent/src_main_java_org_apache_commons_math_util_FastMath.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/69/parent/src_main_java_org_apache_commons_math_stat_correlation_PearsonsCorrelation.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/70/parent/src_main_java_org_apache_commons_math_analysis_solvers_BisectionSolver.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/75/parent/src_main_java_org_apache_commons_math_stat_Frequency.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Math/80/parent/src_main_java_org_apache_commons_math_linear_EigenDecompositionImpl.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Mockito/5/parent/src_org_mockito_internal_verification_VerificationOverTimeImpl.java",
			"/zf8/sc2nf/CCRecom_exp/Defects4j/Time/4/parent/src_main_java_org_joda_time_Partial.java"};
	public static List<String> oneLineBugs = null;
	static {
		oneLineBugs = Arrays.asList(ALL_ONE_LINE_BUGS);
	}

}
