function pd1 = createLogLogitFit(wtValsEx)
%CREATEFIT    Create plot of datasets and fits
%   PD1 = CREATEFIT(WTVALSEX)
%   Creates a plot, similar to the plot in the main distribution fitting
%   window, using the data that you provide as input.  You can
%   apply this function to the same data you used with dfittool
%   or with different data.  You may want to edit the function to
%   customize the code and this help message.
%
%   Number of datasets:  1
%   Number of fits:  1
%
%   See also FITDIST.

% This function was automatically generated on 08-Dec-2015 15:10:57

% Output fitted probablility distribution: PD1

% Data from dataset "wtValsEx data":
%    Y = wtValsEx

% Force all inputs to be column vectors
wtValsEx = wtValsEx(:);

% Prepare figure
clf;
hold on;
LegHandles = []; LegText = {};


% --- Plot data originally in dataset "wtValsEx data"
[CdfF,CdfX] = ecdf(wtValsEx,'Function','cdf');  % compute empirical cdf
BinInfo.rule = 1;
[~,BinEdge] = internal.stats.histbins(wtValsEx,[],[],BinInfo,CdfF,CdfX);
[BinHeight,BinCenter] = ecdfhist(CdfF,CdfX,'edges',BinEdge);
hScat = scatter(BinCenter, BinHeight,  65, [0 .05 .75], 'filled');
xlabel('Exctitatory Synaptic Efficacy');
set(gca, 'XScale', 'log');
ylabel('Density');
LegHandles(end+1) = hScat;
LegText{end+1} = '\fontfamily{phv} \selectfont Ex. Efficacies';


% Create grid where function will be computed
XLim = get(gca,'XLim');
XLim = XLim + [-1 1] * 0.01 * diff(XLim);
XGrid = logspace(log10(XLim(1)),log10(XLim(2)),1000);


% --- Create fit "fit 1"

% Fit this distribution to get parameter values
% To use parameter estimates from the original fit:
%     pd1 = ProbDistUnivParam('loglogistic',[ -1.061749286433, 0.1579349164038])
pd1 = fitdist(wtValsEx, 'loglogistic');
YPlot = pdf(pd1,XGrid);
hLine = plot(XGrid,YPlot,'Color',[.2 .8 .1],...
    'LineStyle','-', 'LineWidth',2,...
    'Marker','none', 'MarkerSize',6);
LegHandles(end+1) = hLine;
LegText{end+1} = strcat('\fontfamily{phv} \selectfont Log-Logistic: $\mu = ', num2str(pd1.mu), ...
    ',\; \sigma = ', num2str(pd1.sigma), '$');

% Fit this distribution to get parameter values
% To use parameter estimates from the original fit:
%     pd1 = ProbDistUnivParam('loglogistic',[ -1.061749286433, 0.1579349164038])
pd1 = fitdist(wtValsEx, 'lognormal');
YPlot = pdf(pd1,XGrid);
hLine = plot(XGrid,YPlot,'Color',[.8 .1 0],...
    'LineStyle','-', 'LineWidth',2,...
    'Marker','none', 'MarkerSize',6);
LegHandles(end+1) = hLine;
LegText{end+1} = strcat('\fontfamily{phv} \selectfont Log $\mathcal{N}:\;\mu = ', num2str(pd1.mu), ...
    ',\; \sigma = ', num2str(pd1.sigma), '$');

% Adjust figure
box on;
hold off;

% Create legend from accumulated handles and labels
hLegend = legend(LegHandles,LegText,'Orientation', 'vertical', 'FontSize', 9, 'Location', 'northeast');
set(hLegend,'Interpreter','latex');
