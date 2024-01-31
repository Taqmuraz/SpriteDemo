import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

interface Drawing
{
    void draw(Graphics2D graphics);
}

interface Sprite
{
    Drawing drawing(Point2D.Float center, Point2D.Float size);

    static Sprite fromImage(Image image, int srcX, int srcY, int srcW, int srcH)
    {
        return (center, size) -> graphics ->
        {
            AffineTransform state = graphics.getTransform();
            graphics.translate(center.x - size.x * 0.5, center.y - size.y * 0.5);
            graphics.scale(size.x, size.y);
            graphics.drawImage(image, 0, 0, 1, 1, srcX, srcY, srcX + srcW, srcY + srcH, null);
            graphics.setTransform(state);
        };
    }
}

interface Animator
{
    Animation animation(int index);

    static Animator fromImage(Image image, int rows, int lines)
    {
        Animation[] animations = new Animation[lines];
        for(int i = 0; i < animations.length; i++)
        {
            animations[i] = Animation.fromImage(image, rows, lines, i * rows, rows);
        }
        return index -> animations[index];
    }
}

interface Animation
{
    Sprite animate(float normalizedTime);

    static Animation fromImage(Image image, int rows, int lines, int offset, int frames)
    {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        int elementWidth = width / rows;
        int elementHeight = height / lines;
        int elements = frames;
        Sprite[] sprites = new Sprite[elements];
        for(int i = 0; i < elements; i++)
        {
            int x = (i + offset) % rows;
            int y = (i + offset) / rows;
            sprites[i] = Sprite.fromImage(image, x * elementWidth, y * elementHeight, elementWidth, elementHeight);
        }
        return fromSprites(sprites);
    }
    static Animation fromSprites(Sprite... sprites)
    {
        return time -> sprites[(int)Math.max(0, sprites.length * time) % sprites.length];
    }
}

class InputListener implements KeyListener
{
    boolean[] wasd = new boolean[4];
    final int W = 0, A = 1, S = 2, D = 3;
    Map<Character, Consumer<Boolean>> actions = Map.of(
        'w', k -> wasd[W] = k,
        'a', k -> wasd[A] = k,
        's', k -> wasd[S] = k,
        'd', k -> wasd[D] = k);

    void action(char character, boolean status)
    {
        var handler = actions.get(character);
        if(handler != null) handler.accept(status);
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        action(e.getKeyChar(), true);
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        action(e.getKeyChar(), false);
    }

    public Point2D.Float input()
    {
        float x = 0f;
        float y = 0f;
        if(wasd[W]) y++;
        if(wasd[A]) x--;
        if(wasd[S]) y--;
        if(wasd[D]) x++;
        return new Point2D.Float(x, y);
    }
    public boolean any()
    {
        boolean any = false;
        for(int i = 0; i < wasd.length; i++)
        {
            if(wasd[i])
            {
                any = true;
                break;
            }
        }
        return any;
    }
}

class Panel extends JPanel
{
    Drawing drawing;

    Panel(Drawing drawing)
    {
        this.drawing = drawing;
    }

    @Override
    public void paint(Graphics graphics)
    {
        graphics.setColor(Color.black);
        graphics.fillRect(0, 0, getWidth(), getHeight());
        drawing.draw((Graphics2D)graphics);
        repaint();
    }
}

class Window extends JFrame
{
    Panel panel;

    Window(int width, int height, Drawing drawing)
    {
        panel = new Panel(drawing);
        setSize(width, height);
        add(panel);
    }
    @Override
    public void setSize(int width, int height)
    {
        super.setSize(width, height);
        panel.setSize(width, height);
    }
}

public class Program
{
    static float time()
    {
        return (System.currentTimeMillis() % 86_400_000) * 0.001f;
    }

    public static void main(String[] args)
    {
        Animator walkAnim, idleAnim;
        InputListener input = new InputListener();
        int[] playerAngle = { 0 };
        float[] playerPosition = { 0f, 0f };
        float[] lastTime = { time() };
        try
        {
            Image walk = ImageIO.read(new FileInputStream(new File("Walk.png")));
            Image idle = ImageIO.read(new FileInputStream(new File("idle.png")));
            walkAnim = Animator.fromImage(walk, 18, 8);
            idleAnim = Animator.fromImage(idle, 1, 8);
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
        Window window = new Window(800, 800, graphics ->
        {
            float time = time();
            float deltaTime = time - lastTime[0];
            lastTime[0] = time;

            Point2D.Float wasd = input.input();
            float x = wasd.x;
            float y = wasd.y;
            float len = (float)Math.sqrt(x * x + y * y);
            Animator player = input.any() ? walkAnim : idleAnim;
            if(len != 0f)
            {
                x /= len;
                y /= len;
                float angle = (float)Math.acos(x) * (y > 0 ? -1f : 1f) * 180f / (float)Math.PI;
                playerAngle[0] = (((int)angle + 360) % 360 / 45 + 7) % 8;
                playerPosition[0] += x * 300f * deltaTime;
                playerPosition[1] -= y * 300f * deltaTime;
            }

            player.animation(playerAngle[0]).animate(time).drawing(
                new Point2D.Float(400f + playerPosition[0], 400f + playerPosition[1]),
                new Point2D.Float(300f, 300f)).draw(graphics);
        });
        window.addKeyListener(input);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }
}