
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.RowCoder;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.transforms.SerializableFunctions;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.Row;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.extensions.sql.SqlTransform;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.schemas.Schema;

public class SQLJoinExample {

    final static String order_header = "userId,orderId,productId,Amount";
    final static Schema order_schema = Schema.builder()
            .addStringField("userId")
            .addStringField("orderId")
            .addStringField("productId")
            .addDoubleField("Amount").build();

    final static String user_header = "userId,name";
    final static Schema user_schema = Schema.builder()
            .addStringField("userId")
            .addStringField("name").build();

    final static Schema order_user_schema = Schema.builder()
            .addStringField("userId")
            .addStringField("orderId")
            .addStringField("productId")
            .addDoubleField("Amount").addStringField("name").build();

    public static void main(String[] args) {
        // TODO Auto-generated method stub

        Pipeline pipeline = Pipeline.create();

        // Step 1 : Read csv file.

        PCollection<String> order = pipeline.apply(TextIO.read().from("C:\\Beam\\user_order.csv"));
        PCollection<String> user = pipeline.apply(TextIO.read().from("C:\\Beam\\p_user.csv"));

        // Step 2 : Convert PCollection<String> to PCollection<Row>

        PCollection<Row> rowUserOrder = order.apply(ParDo.of(new StringToOrderRow()))
                .setRowSchema(order_schema);
        PCollection<Row> rowUser = user.apply(ParDo.of(new StringToUserRow()))
                .setRowSchema(user_schema);

        // Step 3 : Apply SqlTramsform.query

        PCollection<Row> sqlInput = PCollectionTuple.of(new TupleTag<>("orders"), rowUserOrder)
                .and(new TupleTag<>("users"), rowUser)
                .apply(SqlTransform.query("select o.*,u.name from orders o inner join users u on o.userId=u.userId"));

        // Step 4 : Convert PCollection<Row> to PCollection<String>

        PCollection<String> pOutput = sqlInput.apply(ParDo.of(new RowToString()));

        pOutput.apply(TextIO.write().to("C:\\Beam\\sql_join_output.csv").withNumShards(1).withSuffix(".csv"));

        pipeline.run();
    }

    // ParDo for String -> Row (SQL)
    public static class StringToOrderRow extends DoFn<String, Row> {
        @ProcessElement
        public void processElement(ProcessContext c) {

            if (!c.element().equalsIgnoreCase(order_header)) {
                String arr[] = c.element().split(",");

                Row record = Row.withSchema(order_schema)
                        .addValues(arr[0], arr[1], arr[2], Double.valueOf(arr[3])).build();
                c.output(record);
            }

        }
    }

    // ParDo for String -> Row (SQL)
    public static class StringToUserRow extends DoFn<String, Row> {
        @ProcessElement
        public void processElement(ProcessContext c) {

            if (!c.element().equalsIgnoreCase(user_header)) {
                String arr[] = c.element().split(",");

                Row record = Row.withSchema(user_schema)
                        .addValues(arr[0], arr[1]).build();
                c.output(record);
            }

        }
    }

    // ParDo for Row (SQL) -> String
    public static class RowToString extends DoFn<Row, String> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            String outString = c.element().getValues().stream()
                    // For Left Join
                    // .filter(entity -> entity != null)
                    .map(Object::toString).collect(Collectors.joining(","));

            c.output(outString);
        }
    }

}