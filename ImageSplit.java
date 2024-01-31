import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

public class ImageSplit
{
    public static void main(String[] args)
    {
        File root = new File(args[0]);
        var files = root.listFiles();
        int number = files.length;
        int rows = Integer.valueOf(args[2]);
        int lines = (int)Math.ceil(number / rows);
        var images = Stream.of(files).sorted((a, b) -> a.getName().compareTo(b.getName())).map(f ->
        {
            try
            {
                return ImageIO.read(f);
            }
            catch(Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }).toArray(BufferedImage[]::new);
        int elementWidth = images[0].getWidth();
        int elementHeight = images[0].getHeight();
        int width = elementWidth * rows;
        int height = elementHeight * lines;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D)result.getGraphics();
        graphics.setColor(new Color(0, 0, 0, 0));
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.white);

        for(int i = 0; i < number; i++)
        {
            int x = i % rows;
            int y = i / rows;
            graphics.drawImage(images[i], x * elementWidth, y * elementHeight, elementWidth, elementHeight, null);
        }

        try
        {
            ImageIO.write(result, "png", new FileOutputStream(new File(args[1])));
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
