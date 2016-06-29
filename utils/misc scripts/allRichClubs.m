function allRichClubs ( wtMat, wtMatee, wtMatee10, nullMods, nullModsee, nullModsee10 )
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here
    figure;
    [kIn, kOut, k] = nodeDegrees(wtMat);
    subplot(3, 2, 1);
    hold on;
    richClubDir(wtMat, kIn, nullMods, 'both', 1);
    hold off;
    title('In-Rich Club: All Wts')
    
    subplot(3, 2, 2);
    hold on;
    richClubDir(wtMat, kOut, nullMods, 'both', 1);
    hold off;
    title('Out-Rich Club: All Wts')
    
    [kInee, kOutee, ke] = nodeDegrees(wtMatee);
    subplot(3, 2, 3);
    hold on;
    richClubDir(wtMatee, kInee, nullModsee, 'both', 1);
    hold off;
    title('In-Rich-Club: Ex Only')
    
    subplot(3, 2, 4);
    hold on;
    richClubDir(wtMatee, kOutee, nullModsee, 'both', 1);
    hold off;
    title('Out-Rich-Club: Ex Only')
    
    [kIn10, kOut10, k10] = nodeDegrees(wtMatee10); 
    subplot(3, 2, 5);
    hold on;
    richClubDir(wtMatee10, kIn10, nullModsee10, 'both', 1);
    hold off;
    title('In-Rich Club: Top 10%')
    
    subplot(3, 2, 6)
    hold on;
    richClubDir(wtMatee10, kOut10, nullModsee10, 'both', 1);
    hold off;
    title('Out-Rich Club: Top 10%')
    
    richClubDir(wtMatee, ke, nullModsee, 'both', 1);
    title('Ex-Ex Degree Rich-Club', 'FontSize', 18);
    
    figure;
    richClubDir(wtMatee10, k10, nullModsee10, 'both', 1);
    title('Ex-Ex RC Top 10% of synapses only', ...
    'FontSize', 16);
    
end

