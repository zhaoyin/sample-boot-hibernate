package sample.context.orm;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.hibernate.criterion.MatchMode;

/**
 * 簡易にJPQLを生成するためのビルダー。
 * <p>条件句の動的条件生成に特化させています。
 */
public class JpqlBuilder {

    private final StringBuilder jpql;
    private final AtomicInteger index;
    private final MutableList<String> conditions = Lists.mutable.empty();
    private final MutableList<Object> reservedArgs = Lists.mutable.empty();
    private final MutableList<Object> args = Lists.mutable.empty();
    private Optional<String> orderBy = Optional.empty();

    public JpqlBuilder(String baseJpql, int fromIndex) {
        this.jpql = new StringBuilder(baseJpql);
        this.index = new AtomicInteger(fromIndex);
    }

    public JpqlBuilder(String baseJpql, String staticCondition, int fromIndex) {
        this(baseJpql, fromIndex);
        add(staticCondition);
    }

    private JpqlBuilder add(String condition) {
        if (StringUtils.isNotBlank(condition)) {
            this.conditions.add(condition);
        }
        return this;
    }

    private JpqlBuilder reservedArgs(Object... args) {
        if (args != null) {
            this.reservedArgs.addAll(Arrays.asList(args));
        }
        return this;
    }

    /** 一致条件を付与します。(値がnullの時は無視されます) */
    public JpqlBuilder equal(String field, Object value) {
        return ifValid(value, () -> {
            conditions.add(String.format("%s = ?%d", field, index.getAndIncrement()));
            args.add(value);
        });
    }

    private JpqlBuilder ifValid(Object value, Runnable command) {
        if (isValid(value)) {
            command.run();
        }
        return this;
    }

    private boolean isValid(Object value) {
        if (value instanceof String) {
            return StringUtils.isNotBlank((String) value);
        } else if (value instanceof Optional) {
            return ((Optional<?>) value).isPresent();
        } else if (value instanceof Object[]) {
            return value != null && 0 < ((Object[]) value).length;
        } else if (value instanceof Collection) {
            return value != null && 0 < ((Collection<?>) value).size();
        } else {
            return value != null;
        }
    }

    /** 不一致条件を付与します。(値がnullの時は無視されます) */
    public JpqlBuilder equalNot(String field, Object value) {
        return ifValid(value, () -> {
            conditions.add(String.format("%s != ?%d", field, index.getAndIncrement()));
            args.add(value);
        });
    }

    /** like条件を付与します。(値がnullの時は無視されます) */
    public JpqlBuilder like(String field, String value, MatchMode mode) {
        return ifValid(value, () -> {
            conditions.add(String.format("%s like ?%d", field, index.getAndIncrement()));
            args.add(mode.toMatchString(value));
        });
    }

    /** like条件を付与します。[複数フィールドに対するOR結合](値がnullの時は無視されます) */
    public JpqlBuilder like(List<String> fields, String value, MatchMode mode) {
        return ifValid(value, () -> {
            StringBuilder condition = new StringBuilder("(");
            for (String field : fields) {
                if (condition.length() != 1) {
                    condition.append(" or ");
                }
                condition.append(String.format("(%s like ?%d)", field, index.getAndIncrement()));
                args.add(mode.toMatchString(value));
            }
            condition.append(")");
            conditions.add(condition.toString());
        });
    }

    /** in条件を付与します。 */
    public JpqlBuilder in(String field, List<Object> values) {
        return ifValid(values, () -> {
            conditions.add(String.format("%s in ?%d", field, index.getAndIncrement()));
            args.add(values);
        });
    }

    /** between条件を付与します。 */
    public JpqlBuilder between(String field, Date from, Date to) {
        if (from != null && to != null) {
            conditions.add(String.format(
                    "%s between ?%d and ?%d", field, index.getAndIncrement(), index.getAndIncrement()));
            args.add(from);
            args.add(to);
        }
        return this;
    }

    /** between条件を付与します。 */
    public JpqlBuilder between(String field, LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            conditions.add(String.format(
                    "%s between ?%d and ?%d", field, index.getAndIncrement(), index.getAndIncrement()));
            args.add(from);
            args.add(to);
        }
        return this;
    }

    /** between条件を付与します。 */
    public JpqlBuilder between(String field, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null) {
            conditions.add(String.format(
                    "%s between ?%d and ?%d", field, index.getAndIncrement(), index.getAndIncrement()));
            args.add(from);
            args.add(to);
        }
        return this;
    }

    /** between条件を付与します。 */
    public JpqlBuilder between(String field, String from, String to) {
        if (isValid(from) && isValid(to)) {
            conditions.add(String.format(
                    "%s between ?%d and ?%d", field, index.getAndIncrement(), index.getAndIncrement()));
            args.add(from);
            args.add(to);
        }
        return this;
    }

    /** [フィールド]&gt;=[値] 条件を付与します。(値がnullの時は無視されます) */
    public <Y extends Comparable<? super Y>> JpqlBuilder gte(String field, final Y value) {
        return ifValid(value, () -> {
            conditions.add(String.format("%s >= ?%d", field, index.getAndIncrement()));
            args.add(value);
        });
    }

    /** [フィールド]&gt;[値] 条件を付与します。(値がnullの時は無視されます) */
    public <Y extends Comparable<? super Y>> JpqlBuilder gt(String field, final Y value) {
        return ifValid(value, () -> {
            conditions.add(String.format("%s > ?%d", field, index.getAndIncrement()));
            args.add(value);
        });
    }

    /** [フィールド]&lt;=[値] 条件を付与します。 */
    public <Y extends Comparable<? super Y>> JpqlBuilder lte(String field, final Y value) {
        return ifValid(value, () -> {
            conditions.add(String.format("%s <= ?%d", field, index.getAndIncrement()));
            args.add(value);
        });
    }

    /** [フィールド]&lt;[値] 条件を付与します。 */
    public <Y extends Comparable<? super Y>> JpqlBuilder lt(String field, final Y value) {
        return ifValid(value, () -> {
            conditions.add(String.format("%s < ?%d", field, index.getAndIncrement()));
            args.add(value);
        });
    }

    /** order by 条件句を付与します。 */
    public JpqlBuilder orderBy(String orderBy) {
        this.orderBy = Optional.ofNullable(orderBy);
        return this;
    }

    /** JPQLを生成します。 */
    public String build() {
        StringBuilder jpql = new StringBuilder(this.jpql.toString());
        if (!conditions.isEmpty()) {
            jpql.append(" where ");
            AtomicBoolean first = new AtomicBoolean(true);
            conditions.each(condition -> {
                if (!first.getAndSet(false)) {
                    jpql.append(" and ");
                }
                jpql.append(condition);
            });
        }
        orderBy.ifPresent(v -> jpql.append(" order by " + v));
        return jpql.toString();
    }

    /** JPQLに紐付く実行引数を返します。 */
    public Object[] args() {
        return Lists.mutable.ofAll(reservedArgs).withAll(args).toArray();
    }

    /**
     * ビルダーを生成します。
     * @param baseJpql 基点となるJPQL (where / order by は含めない)
     * @return ビルダー情報
     */
    public static JpqlBuilder of(String baseJpql) {
        return new JpqlBuilder(baseJpql, 1);
    }

    /**
     * ビルダーを生成します。
     * @param baseJpql 基点となるJPQL (where / order by は含めない)
     * @param fromIndex 動的に付与する条件句の開始インデックス(1開始)。
     * 既に「field=?1」等で置換連番を付与しているときはその次番号。
     * @param args 既に付与済みの置換連番に紐づく引数
     * @return ビルダー情報
     */
    public static JpqlBuilder of(String baseJpql, int fromIndex, Object... args) {
        return new JpqlBuilder(baseJpql, fromIndex).reservedArgs(args);
    }

    /**
     * ビルダーを生成します。
     * @param baseJpql 基点となるJPQL (where / order by は含めない)
     * @param staticCondition 条件指定無しに確定する where 条件句 (field is null 等)
     * @return ビルダー情報
     */
    public static JpqlBuilder of(String baseJpql, String staticCondition) {
        return new JpqlBuilder(baseJpql, staticCondition, 1);
    }

    /**
     * ビルダーを生成します。
     * @param baseJpql 基点となるJPQL (where / order by は含めない)
     * @param staticCondition 条件指定無しに確定する where 条件句 (field is null 等)
     * @param fromIndex 動的に付与する条件句の開始インデックス(1開始)。
     * 既に「field=?1」等で置換連番を付与しているときはその次番号。
     * @param args 既に付与済みの置換連番に紐づく引数
     * @return ビルダー情報
     */
    public static JpqlBuilder of(String baseJpql, String staticCondition, int fromIndex, Object... args) {
        return new JpqlBuilder(baseJpql, staticCondition, fromIndex).reservedArgs(args);
    }

}
