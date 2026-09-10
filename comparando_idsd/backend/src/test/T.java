import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.idsd.kanban.internal.projeto.Papel;
public class T {
  public static void main(String[] a) throws Exception {
    ObjectMapper m = new ObjectMapper();
    System.out.println("valido: " + m.readValue("\"project_admin\"", Papel.class));
    try { System.out.println("constante: " + m.readValue("\"PROJECT_ADMIN\"", Papel.class)); }
    catch (Exception e) { System.out.println("constante recusada: " + e.getClass().getSimpleName()); }
    try { m.readValue("\"superusuario\"", Papel.class); }
    catch (Exception e) { System.out.println("desconhecido: " + e.getClass().getName()); }
  }
}
