/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java;

import static net.sourceforge.pmd.lang.ast.Parser.ParserTask;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.SemanticErrorReporter;
import net.sourceforge.pmd.lang.ast.SemanticException;
import net.sourceforge.pmd.lang.ast.test.BaseParsingHelper;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.JavaParser;
import net.sourceforge.pmd.lang.java.internal.JavaAstProcessor;
import net.sourceforge.pmd.lang.java.internal.JavaLanguageHandler;
import net.sourceforge.pmd.lang.java.types.TypeSystem;
import net.sourceforge.pmd.lang.java.types.internal.infer.TypeInferenceLogger;
import net.sourceforge.pmd.lang.java.types.internal.infer.TypeInferenceLogger.SimpleLogger;
import net.sourceforge.pmd.lang.java.types.internal.infer.TypeInferenceLogger.VerboseLogger;

import kotlin.Pair;

public class JavaParsingHelper extends BaseParsingHelper<JavaParsingHelper, ASTCompilationUnit> {

    /**
     * Note: this is the default type system for everything parsed with
     * default options of JavaParsingHelper. This allows constants like
     * the null type to be compared.
     */
    public static final TypeSystem TEST_TYPE_SYSTEM = TypeSystem.usingClassLoaderClasspath(JavaParsingHelper.class.getClassLoader());

    /** This runs all processing stages when parsing. */
    public static final JavaParsingHelper DEFAULT = new JavaParsingHelper(
        Params.getDefault(),
        SemanticErrorReporter.noop(), // todo change this to unforgiving logger, need to update a lot of tests
        TEST_TYPE_SYSTEM,
        TypeInferenceLogger.noop()
    );

    private final SemanticErrorReporter semanticLogger;
    private final TypeSystem ts;
    private final TypeInferenceLogger typeInfLogger;

    private JavaParsingHelper(Params params, SemanticErrorReporter logger, TypeSystem ts, TypeInferenceLogger typeInfLogger) {
        super(JavaLanguageModule.NAME, ASTCompilationUnit.class, params);
        this.semanticLogger = logger;
        this.ts = ts;
        this.typeInfLogger = typeInfLogger;
    }

    @Override
    protected @NonNull ASTCompilationUnit doParse(@NonNull Params params, @NonNull ParserTask task) {
        JavaLanguageHandler handler = (JavaLanguageHandler) task.getLanguageVersion().getLanguageVersionHandler();
        JavaParser parser = handler.getParserWithoutProcessing();
        ASTCompilationUnit rootNode = parser.parse(task);
        if (params.getDoProcess()) {
            JavaAstProcessor.create(ts, task.getLanguageVersion(), semanticLogger, typeInfLogger)
                            .process(rootNode);
        }
        return rootNode;
    }

    public TypeInferenceLogger getTypeInfLogger() {
        return typeInfLogger;
    }

    public JavaParsingHelper withLogger(SemanticErrorReporter logger) {
        return new JavaParsingHelper(this.getParams(), logger, this.ts, this.typeInfLogger);
    }

    public JavaParsingHelper withTypeSystem(TypeSystem ts) {
        return new JavaParsingHelper(this.getParams(), this.semanticLogger, ts, this.typeInfLogger);
    }

    public JavaParsingHelper logTypeInference(boolean verbose, PrintStream out) {
        return logTypeInference(verbose ? new VerboseLogger(out) : new SimpleLogger(out));
    }

    public JavaParsingHelper logTypeInference(TypeInferenceLogger logger) {
        return new JavaParsingHelper(this.getParams(), this.semanticLogger, this.ts, logger);
    }

    @Override
    protected @NonNull JavaParsingHelper clone(@NonNull Params params) {
        return new JavaParsingHelper(params, semanticLogger, ts, typeInfLogger);
    }

    public static class TestCheckLogger implements SemanticErrorReporter {

        public final Map<String, List<kotlin.Pair<Node, Object[]>>> warnings = new HashMap<>();
        public final Map<String, List<kotlin.Pair<Node, Object[]>>> errors = new HashMap<>();

        private final SemanticErrorReporter baseLogger;

        public TestCheckLogger() {
            this(false);
        }

        public TestCheckLogger(boolean doLogOnConsole) {
            if (doLogOnConsole) {
                Logger consoleLogger = LoggerFactory.getLogger(TestCheckLogger.class);
                baseLogger = SemanticErrorReporter.reportToLogger(consoleLogger);
            } else {
                baseLogger = SemanticErrorReporter.noop();
            }
        }

        @Override
        public void warning(Node location, String message, Object... args) {
            warnings.computeIfAbsent(message, k -> new ArrayList<>())
                    .add(new Pair<>(location, args));

            baseLogger.warning(location, message, args);
        }

        @Override
        public SemanticException error(Node location, String message, Object... args) {
            errors.computeIfAbsent(message, k -> new ArrayList<>())
                  .add(new Pair<>(location, args));
            return baseLogger.error(location, message, args);
        }

        @Override
        public boolean hasError() {
            return baseLogger.hasError();
        }
    }

    /**
     * Will throw on the first semantic error or warning.
     * Useful because it produces a stack trace for that warning/error.
     */
    public static class UnforgivingSemanticLogger implements SemanticErrorReporter {

        private static final int MAX_NODE_TEXT_WIDTH = 100;
        public static final UnforgivingSemanticLogger INSTANCE = new UnforgivingSemanticLogger();

        private UnforgivingSemanticLogger() {

        }

        @Override
        public void warning(Node location, String message, Object... formatArgs) {
            String warning = MessageFormat.format(message, formatArgs);
            throw new AssertionError(
                "Expected no warnings, but got: " + warning + "\n"
                    + "at " + StringUtils.truncate(location.toString(), 100)
            );
        }

        @Override
        public SemanticException error(Node location, String message, Object... formatArgs) {
            String error = MessageFormat.format(message, formatArgs);
            throw new AssertionError(
                "Expected no errors, but got: " + error + "\n"
                    + "at " + StringUtils.truncate(location.toString(), MAX_NODE_TEXT_WIDTH)
            );
        }

        @Override
        public boolean hasError() {
            return false;
        }
    }
}
