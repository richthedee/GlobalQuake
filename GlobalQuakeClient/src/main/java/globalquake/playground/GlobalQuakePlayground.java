package globalquake.playground;

import globalquake.client.GlobalQuakeLocal;
import globalquake.core.GlobalQuake;
import globalquake.core.archive.EarthquakeArchive;
import globalquake.core.exception.ApplicationErrorHandler;
import globalquake.core.regions.Regions;
import globalquake.main.Main;
import globalquake.utils.Scale;
import org.tinylog.Logger;

import java.awt.*;

public class GlobalQuakePlayground extends GlobalQuakeLocal {

    public static void main(String[] args) throws Exception{
        GlobalQuake.prepare(Main.MAIN_FOLDER, new ApplicationErrorHandler(null));
        Regions.init();
        Scale.load();

        new GlobalQuakePlayground();
    }

    @Override
    public void startRuntime() {
        getGlobalQuakeRuntime().runThreads();
    }

    public GlobalQuakePlayground() {
        super(new StationDatabaseManagerPlayground());
        createFrame();
        startRuntime();
    }

    public GlobalQuakePlayground createFrame() {
        EventQueue.invokeLater(() -> {
            try {
                globalQuakeFrame = new GlobalQuakeFramePlayground();
                globalQuakeFrame.setVisible(true);

                Main.getErrorHandler().setParent(globalQuakeFrame);
            }catch (Exception e){
                Logger.error(e);
                System.exit(0);
            }
        });
        return this;
    }

    @Override
    public EarthquakeArchive createArchive() {
        return new EarthquakeArchive();
    }
}
