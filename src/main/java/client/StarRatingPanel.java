package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.swing.FontIcon;

public class StarRatingPanel extends JPanel {
    private int currentRating = 5; // Default is 5 stars
    private int hoverRating = 0;
    private final int maxStars = 5;
    private final JLabel[] stars;
    private final JLabel lblScore;

    public StarRatingPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        setOpaque(false);

        stars = new JLabel[maxStars];
        for (int i = 0; i < maxStars; i++) {
            final int starValue = i + 1;
            JLabel star = new JLabel(FontIcon.of(Feather.STAR, 32, UITheme.BORDER));
            star.setCursor(new Cursor(Cursor.HAND_CURSOR));

            star.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hoverRating = starValue;
                    updateStars();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverRating = 0;
                    updateStars();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    currentRating = starValue;
                    updateStars();
                }
            });

            stars[i] = star;
            add(star);
        }
        
        lblScore = new JLabel("5 sao");
        lblScore.setFont(UITheme.fontBold(14));
        lblScore.setForeground(UITheme.TEXT_PRIMARY);
        lblScore.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        add(lblScore);
        
        updateStars();
    }

    private void updateStars() {
        int activeRating = hoverRating > 0 ? hoverRating : currentRating;
        for (int i = 0; i < maxStars; i++) {
            if (i < activeRating) {
                stars[i].setIcon(FontIcon.of(Feather.STAR, 32, UITheme.AMBER));
            } else {
                stars[i].setIcon(FontIcon.of(Feather.STAR, 32, UITheme.BORDER));
            }
        }
        if (lblScore != null) {
            lblScore.setText(activeRating + " sao");
        }
    }

    public int getRating() {
        return currentRating;
    }
}
